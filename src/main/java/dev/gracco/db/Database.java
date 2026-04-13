package dev.gracco.db;

import dev.gracco.ui.Alert;
import dev.gracco.util.Validation;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static Connection connection;

    public static boolean initialize() {
        Dotenv dotenv = Dotenv.load();
        String DB_URL = dotenv.get("DB_URL");
        String DB_USER = dotenv.get("DB_USER");
        String DB_PASSWORD = dotenv.get("DB_PASSWORD");

        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            Alert.fatalError(e.getMessage());
        }

        return true;
    }

    public static void shutdown() {
        try {
            connection.close();
        } catch (SQLException e) {
            Alert.fatalError(e.getMessage());
        }
    }

    public static class Logs {
        private static final int PAGE_SIZE = 10;

        public static void logToDatabase(Enums.ActionType actionType, Enums.Tables tableName, Integer recordId, String description) {
            String sql = """
                    INSERT INTO activity_logs (user_id, action_type, table_name, record_id, action_description)
                    VALUES (?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, User.getUserId());
                statement.setString(2, actionType.name());
                statement.setString(3, tableName.name());

                if (recordId == null) {
                    statement.setNull(4, java.sql.Types.INTEGER);
                } else {
                    statement.setInt(4, recordId);
                }

                statement.setString(5, description);
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save activity log: " + e.getMessage(), e);
            }
        }

        public static Object[][] getLogsPage(int page) {
            if (page < 0) {
                throw new IllegalArgumentException("Page cannot be negative");
            }

            String sql = """
                    SELECT
                        al.log_id,
                        al.user_id,
                        u.username,
                        u.first_name,
                        u.last_name,
                        al.action_type,
                        al.table_name,
                        al.record_id,
                        al.action_description,
                        al.action_timestamp
                    FROM activity_logs al
                    INNER JOIN users u ON al.user_id = u.user_id
                    ORDER BY al.action_timestamp DESC, al.log_id DESC
                    LIMIT 10 OFFSET ?
                    """;

            List<Object[]> rows = new ArrayList<>();

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, page * PAGE_SIZE);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        rows.add(new Object[]{
                                resultSet.getInt("log_id"),
                                resultSet.getInt("user_id"),
                                resultSet.getString("username"),
                                resultSet.getString("first_name"),
                                resultSet.getString("last_name"),
                                resultSet.getString("action_type"),
                                resultSet.getString("table_name"),
                                resultSet.getObject("record_id"),
                                resultSet.getString("action_description"),
                                Validation.formatDateTime(resultSet.getTimestamp("action_timestamp"))
                        });
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to fetch logs: " + e.getMessage(), e);
            }

            return rows.toArray(new Object[0][]);
        }
    }

    public static class User {
        @Getter
        private static int userId;

        @Getter
        private static String username;

        @Getter
        private static String firstName;

        @Getter
        private static String lastName;

        @Getter
        private static boolean changedPassword;

        @Getter
        private static Enums.Role role;

        public static String login(String usernameInput, String password) {
            if (usernameInput.isEmpty() || password.isEmpty()) {
                return "Please enter credentials!";
            }

            String sql = """
                    SELECT user_id, username, first_name, last_name, changed_pass, is_active, role_id, password_hash
                    FROM users
                    WHERE username = ?
                    LIMIT 1
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, usernameInput);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return "Incorrect username or password.";
                    }

                    String passwordHash = resultSet.getString("password_hash");

                    if (!Encryption.decrypt(password, passwordHash)) {
                        return "Incorrect username or password.";
                    }

                    if (!resultSet.getBoolean("is_active")) {
                        return "User is set as inactive. Contact your administrator";
                    }

                    userId = resultSet.getInt("user_id");
                    username = resultSet.getString("username");
                    firstName = resultSet.getString("first_name");
                    lastName = resultSet.getString("last_name");
                    changedPassword = resultSet.getBoolean("changed_pass");
                    role = Enums.Role.fromString(resultSet.getString("role_id"));

                    return null;
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return null;
            }
        }

        public static boolean changePassword(String newPassword) {
            if (newPassword == null || newPassword.isBlank()) {
                Alert.fatalError("Password cannot be empty.");
            }

            String sql = """
                    UPDATE users
                    SET password_hash = ?, changed_pass = TRUE
                    WHERE user_id = ?
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, Encryption.encrypt(newPassword));
                statement.setInt(2, userId);

                int rowsUpdated = statement.executeUpdate();

                if (rowsUpdated == 0) {
                    return false;
                }

                changedPassword = true;

                Logs.logToDatabase(
                        Enums.ActionType.UPDATE,
                        Enums.Tables.USERS,
                        userId,
                        "User changed their password"
                );

                return true;
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return false;
            }
        }
    }

    public static class Admin {
        private static final int PAGE_SIZE = 10;

        public static Object[][] getAdmins(int page) {
            if (page < 0) {
                throw new IllegalArgumentException("Page cannot be negative");
            }

            String sql = """
                    SELECT user_id, username, changed_pass, role_id, first_name, last_name, email, contact_number, is_active, created_at, updated_at
                    FROM users
                    ORDER BY user_id ASC
                    LIMIT ? OFFSET ?
                    """;

            List<Object[]> rows = new ArrayList<>();

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, PAGE_SIZE);
                statement.setInt(2, page * PAGE_SIZE);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        rows.add(new Object[]{
                                resultSet.getInt("user_id"),
                                resultSet.getString("username"),
                                resultSet.getBoolean("changed_pass"),
                                resultSet.getString("role_id"),
                                resultSet.getString("first_name"),
                                resultSet.getString("last_name"),
                                resultSet.getString("email"),
                                resultSet.getString("contact_number"),
                                resultSet.getBoolean("is_active"),
                                Validation.formatDateTime(resultSet.getTimestamp("created_at")),
                                Validation.formatDateTime(resultSet.getTimestamp("updated_at"))
                        });
                    }
                }

                return rows.toArray(new Object[0][]);
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return new Object[0][0];
            }
        }

        public static Object[] getUserById(int userId) {
            String sql = """
                    SELECT user_id, username, changed_pass, role_id, first_name, last_name, email, contact_number, is_active
                    FROM users
                    WHERE user_id = ?
                    LIMIT 1
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }

                    return new Object[]{
                            resultSet.getInt("user_id"),
                            resultSet.getString("username"),
                            resultSet.getBoolean("changed_pass"),
                            resultSet.getString("role_id"),
                            resultSet.getString("first_name"),
                            resultSet.getString("last_name"),
                            resultSet.getString("email"),
                            resultSet.getString("contact_number"),
                            resultSet.getBoolean("is_active")
                    };
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return null;
            }
        }

        public static String addUser(String usernameInput, String password, Enums.Role roleInput,
                                     String firstNameInput, String lastNameInput,
                                     String emailInput, String contactNumberInput) {
            String sql = """
                    INSERT INTO users (
                        username,
                        password_hash,
                        changed_pass,
                        role_id,
                        first_name,
                        last_name,
                        email,
                        contact_number,
                        is_active
                    )
                    VALUES (?, ?, FALSE, ?, ?, ?, ?, ?, TRUE)
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, usernameInput);
                statement.setString(2, Encryption.encrypt(password));
                statement.setString(3, roleInput.toString());
                statement.setString(4, firstNameInput);
                statement.setString(5, lastNameInput);
                statement.setString(6, emailInput);
                statement.setString(7, contactNumberInput);

                int rowsInserted = statement.executeUpdate();

                if (rowsInserted == 0) {
                    return "Failed to add user.";
                }

                Logs.logToDatabase(
                        Enums.ActionType.CREATE,
                        Enums.Tables.USERS,
                        0,
                        "Added user: " + usernameInput
                );

                return null;
            } catch (SQLException e) {
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                    return "Username or email already exists.";
                }

                Alert.fatalError(e.getMessage());
                return "Database error.";
            }
        }

        public static String updateUser(int userId, String usernameInput, String password,
                                        boolean changedPassInput, Enums.Role roleInput,
                                        String firstNameInput, String lastNameInput,
                                        String emailInput, String contactNumberInput,
                                        boolean isActiveInput) {
            String sqlWithPassword = """
                    UPDATE users
                    SET username = ?,
                        password_hash = ?,
                        changed_pass = ?,
                        role_id = ?,
                        first_name = ?,
                        last_name = ?,
                        email = ?,
                        contact_number = ?,
                        is_active = ?
                    WHERE user_id = ?
                    """;

            String sqlWithoutPassword = """
                    UPDATE users
                    SET username = ?,
                        changed_pass = ?,
                        role_id = ?,
                        first_name = ?,
                        last_name = ?,
                        email = ?,
                        contact_number = ?,
                        is_active = ?
                    WHERE user_id = ?
                    """;

            try (PreparedStatement statement = connection.prepareStatement(
                    password == null || password.isBlank() ? sqlWithoutPassword : sqlWithPassword
            )) {
                if (password == null || password.isBlank()) {
                    statement.setString(1, usernameInput);
                    statement.setBoolean(2, changedPassInput);
                    statement.setString(3, roleInput.toString());
                    statement.setString(4, firstNameInput);
                    statement.setString(5, lastNameInput);
                    statement.setString(6, emailInput);
                    statement.setString(7, contactNumberInput);
                    statement.setBoolean(8, isActiveInput);
                    statement.setInt(9, userId);
                } else {
                    statement.setString(1, usernameInput);
                    statement.setString(2, Encryption.encrypt(password));
                    statement.setBoolean(3, changedPassInput);
                    statement.setString(4, roleInput.toString());
                    statement.setString(5, firstNameInput);
                    statement.setString(6, lastNameInput);
                    statement.setString(7, emailInput);
                    statement.setString(8, contactNumberInput);
                    statement.setBoolean(9, isActiveInput);
                    statement.setInt(10, userId);
                }

                int rowsUpdated = statement.executeUpdate();

                if (rowsUpdated == 0) {
                    return "Failed to update user.";
                }

                Logs.logToDatabase(
                        Enums.ActionType.UPDATE,
                        Enums.Tables.USERS,
                        userId,
                        "Updated user: " + usernameInput
                );

                return null;
            } catch (SQLException e) {
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                    return "Username or email already exists.";
                }

                Alert.fatalError(e.getMessage());
                return "Database error.";
            }
        }
    }

    public static class Patient {
        private static final int PAGE_SIZE = 10;

        public static Object[][] getPatients(int page) {
            return getPatients(
                    page,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            );
        }

        public static Object[][] getPatients(int page, String patientId, String firstName, String lastName,
                                             String birthDate, String sex, String contactNumber,
                                             String email, String address) {
            if (page < 0) {
                throw new IllegalArgumentException("Page cannot be negative");
            }

            StringBuilder sql = new StringBuilder("""
                SELECT patient_id, first_name, last_name, birth_date, sex, contact_number, email, address, created_at, updated_at
                FROM patients
                WHERE 1 = 1
                """);

            List<Object> parameters = new ArrayList<>();

            if (!patientId.isBlank()) {
                sql.append(" AND CAST(patient_id AS CHAR) LIKE ?");
                parameters.add("%" + patientId + "%");
            }

            if (!firstName.isBlank()) {
                sql.append(" AND first_name LIKE ?");
                parameters.add("%" + firstName + "%");
            }

            if (!lastName.isBlank()) {
                sql.append(" AND last_name LIKE ?");
                parameters.add("%" + lastName + "%");
            }

            if (!birthDate.isBlank()) {
                sql.append(" AND birth_date = ?");
                parameters.add(Date.valueOf(birthDate));
            }

            if (!sex.isBlank()) {
                sql.append(" AND sex = ?");
                parameters.add(sex);
            }

            if (!contactNumber.isBlank()) {
                sql.append(" AND contact_number LIKE ?");
                parameters.add("%" + contactNumber + "%");
            }

            if (!email.isBlank()) {
                sql.append(" AND email LIKE ?");
                parameters.add("%" + email + "%");
            }

            if (!address.isBlank()) {
                sql.append(" AND address LIKE ?");
                parameters.add("%" + address + "%");
            }

            sql.append(" ORDER BY patient_id ASC LIMIT ? OFFSET ?");
            parameters.add(PAGE_SIZE);
            parameters.add(page * PAGE_SIZE);

            List<Object[]> rows = new ArrayList<>();

            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                for (int i = 0; i < parameters.size(); i++) {
                    statement.setObject(i + 1, parameters.get(i));
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        rows.add(new Object[]{
                                resultSet.getInt("patient_id"),
                                resultSet.getString("first_name"),
                                resultSet.getString("last_name"),
                                Validation.formatDate(resultSet.getDate("birth_date")),
                                resultSet.getString("sex"),
                                resultSet.getString("contact_number"),
                                resultSet.getString("email"),
                                resultSet.getString("address"),
                                Validation.formatDateTime(resultSet.getTimestamp("created_at")),
                                Validation.formatDateTime(resultSet.getTimestamp("updated_at"))
                        });
                    }
                }

                return rows.toArray(new Object[0][]);
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return new Object[0][0];
            }
        }

        public static Object[] getPatientById(int patientId) {
            String sql = """
                SELECT patient_id, first_name, last_name, birth_date, sex, contact_number, email, address
                FROM patients
                WHERE patient_id = ?
                LIMIT 1
                """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, patientId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }

                    return new Object[]{
                            resultSet.getInt("patient_id"),
                            resultSet.getString("first_name"),
                            resultSet.getString("last_name"),
                            resultSet.getDate("birth_date"),
                            resultSet.getString("sex"),
                            resultSet.getString("contact_number"),
                            resultSet.getString("email"),
                            resultSet.getString("address")
                    };
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return null;
            }
        }

        public static String addPatient(String firstName, String lastName, Date birthDate, String sex,
                                        String contactNumber, String email, String address) {
            String sql = """
                INSERT INTO patients (
                    first_name,
                    last_name,
                    birth_date,
                    sex,
                    contact_number,
                    email,
                    address
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, firstName);
                statement.setString(2, lastName);
                statement.setDate(3, birthDate);
                statement.setString(4, sex);
                statement.setString(5, contactNumber);
                statement.setString(6, email);
                statement.setString(7, address);

                int rowsInserted = statement.executeUpdate();

                if (rowsInserted == 0) {
                    return "Failed to add patient.";
                }

                Logs.logToDatabase(
                        Enums.ActionType.CREATE,
                        Enums.Tables.PATIENTS,
                        0,
                        "Added patient: " + firstName + " " + lastName
                );

                return null;
            } catch (SQLException e) {
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                    return "Patient already exists.";
                }

                Alert.fatalError(e.getMessage());
                return "Database error.";
            }
        }

        public static String updatePatient(int patientId, String firstName, String lastName, Date birthDate,
                                           String sex, String contactNumber, String email, String address) {
            String sql = """
                UPDATE patients
                SET first_name = ?, last_name = ?, birth_date = ?, sex = ?, contact_number = ?, email = ?, address = ?
                WHERE patient_id = ?
                """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, firstName);
                statement.setString(2, lastName);
                statement.setDate(3, birthDate);
                statement.setString(4, sex);
                statement.setString(5, contactNumber.isBlank() ? null : contactNumber);
                statement.setString(6, email.isBlank() ? null : email);
                statement.setString(7, address.isBlank() ? null : address);
                statement.setInt(8, patientId);

                int rowsUpdated = statement.executeUpdate();

                if (rowsUpdated == 0) {
                    return "Patient not found.";
                }

                Logs.logToDatabase(
                        Enums.ActionType.UPDATE,
                        Enums.Tables.PATIENTS,
                        patientId,
                        "Updated patient: " + firstName + " " + lastName
                );

                return null;
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return "Database error.";
            }
        }
    }

    public static class Appointment {
        private static final int PAGE_SIZE = 10;

        public static Object[][] getAppointments(int page) {
            return getAppointments(
                    page,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            );
        }

        public static Object[][] getAppointments(int page,
                                                 String appointmentId,
                                                 String patientName,
                                                 String dentistName,
                                                 String scheduledDate,
                                                 String scheduledTime,
                                                 String status,
                                                 String reasonForVisit,
                                                 String notes,
                                                 String createdBy,
                                                 String updatedBy) {
            if (page < 0) {
                throw new IllegalArgumentException("Page cannot be negative");
            }

            StringBuilder sql = new StringBuilder("""
            SELECT
                a.appointment_id,
                CONCAT(p.first_name, ' ', p.last_name) AS patient_name,
                CONCAT(d.first_name, ' ', d.last_name) AS dentist_name,
                a.scheduled_date,
                a.scheduled_time,
                a.status_id,
                a.reason_for_visit,
                a.notes,
                CONCAT(cb.first_name, ' ', cb.last_name) AS created_by_name,
                CASE
                    WHEN ub.user_id IS NULL THEN NULL
                    ELSE CONCAT(ub.first_name, ' ', ub.last_name)
                END AS updated_by_name,
                a.created_at,
                a.updated_at
            FROM appointments a
            INNER JOIN patients p ON a.patient_id = p.patient_id
            INNER JOIN users d ON a.dentist_user_id = d.user_id
            INNER JOIN users cb ON a.created_by = cb.user_id
            LEFT JOIN users ub ON a.updated_by = ub.user_id
            WHERE 1 = 1
            """);

            List<Object> parameters = new ArrayList<>();

            if (!appointmentId.isBlank()) {
                sql.append(" AND CAST(a.appointment_id AS CHAR) LIKE ?");
                parameters.add("%" + appointmentId + "%");
            }

            if (!patientName.isBlank()) {
                sql.append(" AND CONCAT(p.first_name, ' ', p.last_name) LIKE ?");
                parameters.add("%" + patientName + "%");
            }

            if (!dentistName.isBlank()) {
                sql.append(" AND CONCAT(d.first_name, ' ', d.last_name) LIKE ?");
                parameters.add("%" + dentistName + "%");
            }

            if (!scheduledDate.isBlank()) {
                sql.append(" AND a.scheduled_date = ?");
                parameters.add(Date.valueOf(scheduledDate));
            }

            if (!scheduledTime.isBlank()) {
                sql.append(" AND TIME_FORMAT(a.scheduled_time, '%h:%i %p') = ?");
                parameters.add(scheduledTime.trim().toUpperCase());
            }

            if (!status.isBlank()) {
                sql.append(" AND a.status_id = ?");
                parameters.add(status);
            }

            if (!reasonForVisit.isBlank()) {
                sql.append(" AND a.reason_for_visit LIKE ?");
                parameters.add("%" + reasonForVisit + "%");
            }

            if (!notes.isBlank()) {
                sql.append(" AND a.notes LIKE ?");
                parameters.add("%" + notes + "%");
            }

            if (!createdBy.isBlank()) {
                sql.append(" AND CONCAT(cb.first_name, ' ', cb.last_name) LIKE ?");
                parameters.add("%" + createdBy + "%");
            }

            if (!updatedBy.isBlank()) {
                sql.append(" AND CONCAT(ub.first_name, ' ', ub.last_name) LIKE ?");
                parameters.add("%" + updatedBy + "%");
            }

            sql.append(" ORDER BY a.appointment_id ASC LIMIT ? OFFSET ?");
            parameters.add(PAGE_SIZE);
            parameters.add(page * PAGE_SIZE);

            List<Object[]> rows = new ArrayList<>();

            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                for (int i = 0; i < parameters.size(); i++) {
                    statement.setObject(i + 1, parameters.get(i));
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        rows.add(new Object[]{
                                resultSet.getInt("appointment_id"),
                                resultSet.getString("patient_name"),
                                resultSet.getString("dentist_name"),
                                Validation.formatDate(resultSet.getDate("scheduled_date")),
                                resultSet.getTime("scheduled_time").toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm a")),
                                resultSet.getString("status_id"),
                                resultSet.getString("reason_for_visit"),
                                resultSet.getString("notes"),
                                resultSet.getString("created_by_name"),
                                resultSet.getString("updated_by_name"),
                                Validation.formatDateTime(resultSet.getTimestamp("created_at")),
                                Validation.formatDateTime(resultSet.getTimestamp("updated_at"))
                        });
                    }
                }

                return rows.toArray(new Object[0][]);
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return new Object[0][0];
            }
        }

        public static String addAppointment(int patientId,
                                            int dentistUserId,
                                            Date scheduledDate,
                                            Time scheduledTime,
                                            String status,
                                            String reasonForVisit,
                                            String notes) {
            String validatePatientSql = "SELECT patient_id FROM patients WHERE patient_id = ? LIMIT 1";
            String validateDentistSql = """
        SELECT user_id
        FROM users
        WHERE user_id = ? AND role_id = 'Dentist'
        LIMIT 1
        """;
            String insertSql = """
        INSERT INTO appointments (
            patient_id,
            dentist_user_id,
            scheduled_date,
            scheduled_time,
            status_id,
            reason_for_visit,
            notes,
            created_by
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

            try (PreparedStatement patientStatement = connection.prepareStatement(validatePatientSql)) {
                patientStatement.setInt(1, patientId);
                try (ResultSet resultSet = patientStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        return "Patient ID does not exist.";
                    }
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return "Database error.";
            }

            try (PreparedStatement dentistStatement = connection.prepareStatement(validateDentistSql)) {
                dentistStatement.setInt(1, dentistUserId);
                try (ResultSet resultSet = dentistStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        return "Dentist User ID does not exist or is not a dentist.";
                    }
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return "Database error.";
            }

            try (PreparedStatement statement = connection.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, patientId);
                statement.setInt(2, dentistUserId);
                statement.setDate(3, scheduledDate);
                statement.setTime(4, scheduledTime);
                statement.setString(5, status);
                statement.setString(6, reasonForVisit);

                if (notes == null || notes.isBlank()) {
                    statement.setNull(7, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(7, notes);
                }

                statement.setInt(8, User.getUserId());

                int rowsInserted = statement.executeUpdate();

                if (rowsInserted == 0) {
                    return "Failed to add appointment.";
                }

                int appointmentId = 0;
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        appointmentId = generatedKeys.getInt(1);
                    }
                }

                Logs.logToDatabase(
                        Enums.ActionType.CREATE,
                        Enums.Tables.APPOINTMENTS,
                        appointmentId == 0 ? null : appointmentId,
                        "Added appointment for patient ID " + patientId
                );

                return null;
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return "Database error.";
            }
        }

        public static String getPatientFullNameById(int patientId) {
            String sql = """
        SELECT first_name, last_name
        FROM patients
        WHERE patient_id = ?
        LIMIT 1
        """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, patientId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return "Unknown";
                    }

                    return resultSet.getString("first_name") + " " + resultSet.getString("last_name");
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return "Unknown";
            }
        }

        public static List<Object[]> getDentists() {
            String sql = """
        SELECT user_id, first_name, last_name
        FROM users
        WHERE role_id = 'Dentist' AND is_active = TRUE
        ORDER BY first_name ASC, last_name ASC, user_id ASC
        """;

            List<Object[]> dentists = new ArrayList<>();

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    dentists.add(new Object[]{
                            resultSet.getInt("user_id"),
                            resultSet.getString("first_name") + " " + resultSet.getString("last_name")
                    });
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
            }

            return dentists;
        }

        public static Object[] getAppointmentById(int appointmentId) {
            String sql = """
    SELECT
        a.appointment_id,
        a.patient_id,
        CONCAT(p.first_name, ' ', p.last_name) AS patient_name,
        a.dentist_user_id,
        CONCAT(d.first_name, ' ', d.last_name) AS dentist_name,
        a.scheduled_date,
        a.scheduled_time,
        a.status_id,
        a.reason_for_visit,
        a.notes
    FROM appointments a
    INNER JOIN patients p ON a.patient_id = p.patient_id
    INNER JOIN users d ON a.dentist_user_id = d.user_id
    WHERE a.appointment_id = ?
    LIMIT 1
    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, appointmentId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }

                    return new Object[]{
                            resultSet.getInt("appointment_id"),
                            resultSet.getInt("patient_id"),
                            resultSet.getString("patient_name"),
                            resultSet.getInt("dentist_user_id"),
                            resultSet.getString("dentist_name"),
                            resultSet.getDate("scheduled_date"),
                            resultSet.getTime("scheduled_time"),
                            resultSet.getString("status_id"),
                            resultSet.getString("reason_for_visit"),
                            resultSet.getString("notes")
                    };
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return null;
            }
        }

        public static String updateAppointment(int appointmentId,
                                               int dentistUserId,
                                               Date scheduledDate,
                                               Time scheduledTime,
                                               String status,
                                               String reasonForVisit,
                                               String notes) {
            String validateAppointmentSql = """
    SELECT appointment_id
    FROM appointments
    WHERE appointment_id = ?
    LIMIT 1
    """;

            String validateDentistSql = """
    SELECT user_id
    FROM users
    WHERE user_id = ? AND role_id = 'Dentist'
    LIMIT 1
    """;

            String updateSql = """
    UPDATE appointments
    SET dentist_user_id = ?,
        scheduled_date = ?,
        scheduled_time = ?,
        status_id = ?,
        reason_for_visit = ?,
        notes = ?,
        updated_by = ?
    WHERE appointment_id = ?
    """;

            try (PreparedStatement appointmentStatement = connection.prepareStatement(validateAppointmentSql)) {
                appointmentStatement.setInt(1, appointmentId);
                try (ResultSet resultSet = appointmentStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        return "Appointment not found.";
                    }
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return "Database error.";
            }

            try (PreparedStatement dentistStatement = connection.prepareStatement(validateDentistSql)) {
                dentistStatement.setInt(1, dentistUserId);
                try (ResultSet resultSet = dentistStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        return "Dentist User ID does not exist or is not a dentist.";
                    }
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return "Database error.";
            }

            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setInt(1, dentistUserId);
                statement.setDate(2, scheduledDate);
                statement.setTime(3, scheduledTime);
                statement.setString(4, status);
                statement.setString(5, reasonForVisit);

                if (notes == null || notes.isBlank()) {
                    statement.setNull(6, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(6, notes);
                }

                statement.setInt(7, User.getUserId());
                statement.setInt(8, appointmentId);

                int rowsUpdated = statement.executeUpdate();

                if (rowsUpdated == 0) {
                    return "Failed to update appointment.";
                }

                Logs.logToDatabase(
                        Enums.ActionType.UPDATE,
                        Enums.Tables.APPOINTMENTS,
                        appointmentId,
                        "Updated appointment ID " + appointmentId
                );

                return null;
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return "Database error.";
            }
        }
    }
}