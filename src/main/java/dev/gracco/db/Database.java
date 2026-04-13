package dev.gracco.db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import dev.gracco.ui.Alert;
import dev.gracco.util.Validation;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.AccessLevel;
import lombok.Getter;

public class Database {
    @Getter(AccessLevel.PROTECTED)
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

    public static class Appointment {
        private static final int PAGE_SIZE = 10;

        public static Object[][] getAppointments(int page, Integer dentistFilter) {
            if (page < 0) throw new IllegalArgumentException("Page cannot be negative");

            StringBuilder sql = new StringBuilder("""
                    SELECT a.appointment_id, p.first_name, p.last_name, u.first_name, u.last_name,
                           a.scheduled_date, a.scheduled_time, a.status_id, a.reason_for_visit, a.notes,
                           a.patient_id, a.dentist_user_id
                    FROM appointments a
                    INNER JOIN patients p ON a.patient_id = p.patient_id
                    INNER JOIN users u ON a.dentist_user_id = u.user_id
                    WHERE 1=1
                    """);

            List<Object> params = new ArrayList<>();
            if (dentistFilter != null) {
                sql.append(" AND a.dentist_user_id = ?");
                params.add(dentistFilter);
            }
            sql.append(" ORDER BY a.scheduled_date DESC, a.scheduled_time DESC LIMIT ? OFFSET ?");
            params.add(PAGE_SIZE);
            params.add(page * PAGE_SIZE);

            List<Object[]> rows = new ArrayList<>();
            try (PreparedStatement st = connection.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new Object[]{
                                rs.getInt("appointment_id"),
                                rs.getString(2) + " " + rs.getString(3),
                                rs.getString(4) + " " + rs.getString(5),
                                Validation.formatDate(rs.getDate("scheduled_date")),
                                rs.getString("scheduled_time"),
                                rs.getString("status_id"),
                                rs.getString("reason_for_visit"),
                                rs.getString("notes"),
                                rs.getInt("patient_id"),
                                rs.getInt("dentist_user_id")
                        });
                    }
                }
                return rows.toArray(new Object[0][]);
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return new Object[0][0];
            }
        }

        public static Object[] getAppointmentById(int id) {
            String sql = """
                    SELECT appointment_id, patient_id, dentist_user_id, scheduled_date, scheduled_time,
                           status_id, reason_for_visit, notes
                    FROM appointments WHERE appointment_id = ? LIMIT 1
                    """;
            try (PreparedStatement st = connection.prepareStatement(sql)) {
                st.setInt(1, id);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) return null;
                    return new Object[]{
                            rs.getInt("appointment_id"),
                            rs.getInt("patient_id"),
                            rs.getInt("dentist_user_id"),
                            rs.getDate("scheduled_date"),
                            rs.getString("scheduled_time"),
                            rs.getString("status_id"),
                            rs.getString("reason_for_visit"),
                            rs.getString("notes")
                    };
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return null;
            }
        }

        public static String addAppointment(int patientId, int dentistUserId, java.sql.Date scheduledDate,
                                            String scheduledTime, String status, String reason, String notes) {
            String sql = """
                    INSERT INTO appointments (patient_id, dentist_user_id, scheduled_date, scheduled_time,
                        status_id, reason_for_visit, notes, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement st = connection.prepareStatement(sql)) {
                st.setInt(1, patientId);
                st.setInt(2, dentistUserId);
                st.setDate(3, scheduledDate);
                st.setString(4, scheduledTime);
                st.setString(5, status);
                st.setString(6, reason);
                st.setString(7, notes.isBlank() ? null : notes);
                st.setInt(8, User.getUserId());
                if (st.executeUpdate() == 0) return "Failed to add appointment.";
                Logs.logToDatabase(Enums.ActionType.CREATE, Enums.Tables.APPOINTMENTS, null,
                        "Added appointment for patient ID: " + patientId);
                return null;
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return "Database error.";
            }
        }

        public static String updateAppointment(int appointmentId, int patientId, int dentistUserId,
                                               java.sql.Date scheduledDate, String scheduledTime,
                                               String status, String reason, String notes) {
            String sql = """
                    UPDATE appointments SET patient_id=?, dentist_user_id=?, scheduled_date=?,
                        scheduled_time=?, status_id=?, reason_for_visit=?, notes=?, updated_by=?
                    WHERE appointment_id=?
                    """;
            try (PreparedStatement st = connection.prepareStatement(sql)) {
                st.setInt(1, patientId);
                st.setInt(2, dentistUserId);
                st.setDate(3, scheduledDate);
                st.setString(4, scheduledTime);
                st.setString(5, status);
                st.setString(6, reason);
                st.setString(7, notes.isBlank() ? null : notes);
                st.setInt(8, User.getUserId());
                st.setInt(9, appointmentId);
                if (st.executeUpdate() == 0) return "Failed to update appointment.";
                Logs.logToDatabase(Enums.ActionType.UPDATE, Enums.Tables.APPOINTMENTS, appointmentId,
                        "Updated appointment ID: " + appointmentId);
                return null;
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return "Database error.";
            }
        }

        public static int[] getDashboardStats() {
            String sql = """
                    SELECT
                        SUM(CASE WHEN scheduled_date = CURDATE() THEN 1 ELSE 0 END) AS today,
                        SUM(CASE WHEN scheduled_date = CURDATE() AND status_id = 'Confirmed' THEN 1 ELSE 0 END) AS completed,
                        SUM(CASE WHEN scheduled_date = CURDATE() AND status_id IN ('Booked','Rescheduled') THEN 1 ELSE 0 END) AS remaining,
                        SUM(CASE WHEN scheduled_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY) THEN 1 ELSE 0 END) AS tomorrow,
                        SUM(CASE WHEN scheduled_date = CURDATE() AND status_id = 'Cancelled' THEN 1 ELSE 0 END) AS cancelled,
                        SUM(CASE WHEN scheduled_date = CURDATE() AND status_id = 'No Show' THEN 1 ELSE 0 END) AS noshow
                    FROM appointments
                    """;
            try (PreparedStatement st = connection.prepareStatement(sql);
                 ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new int[]{
                            rs.getInt("today"), rs.getInt("completed"), rs.getInt("remaining"),
                            rs.getInt("tomorrow"), rs.getInt("cancelled"), rs.getInt("noshow")
                    };
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
            }
            return new int[]{0, 0, 0, 0, 0, 0};
        }

        public static Object[][] getTodayAppointments() {
            String sql = """
                    SELECT a.appointment_id, p.first_name, p.last_name, u.first_name, u.last_name,
                           a.scheduled_time, a.status_id, a.reason_for_visit
                    FROM appointments a
                    INNER JOIN patients p ON a.patient_id = p.patient_id
                    INNER JOIN users u ON a.dentist_user_id = u.user_id
                    WHERE a.scheduled_date = CURDATE()
                    ORDER BY a.scheduled_time ASC
                    """;
            List<Object[]> rows = new ArrayList<>();
            try (PreparedStatement st = connection.prepareStatement(sql);
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("appointment_id"),
                            rs.getString(2) + " " + rs.getString(3),
                            rs.getString(4) + " " + rs.getString(5),
                            rs.getString("scheduled_time"),
                            rs.getString("status_id"),
                            rs.getString("reason_for_visit")
                    });
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
            }
            return rows.toArray(new Object[0][]);
        }

        public static Object[][] getDentists() {
            String sql = """
                    SELECT user_id, first_name, last_name FROM users
                    WHERE role_id = 'Dentist' AND is_active = TRUE ORDER BY first_name ASC
                    """;
            List<Object[]> rows = new ArrayList<>();
            try (PreparedStatement st = connection.prepareStatement(sql);
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{rs.getInt("user_id"),
                            rs.getString("first_name") + " " + rs.getString("last_name")});
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
            }
            return rows.toArray(new Object[0][]);
        }

        public static Object[][] getPatientList() {
            String sql = "SELECT patient_id, first_name, last_name FROM patients ORDER BY first_name ASC";
            List<Object[]> rows = new ArrayList<>();
            try (PreparedStatement st = connection.prepareStatement(sql);
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{rs.getInt("patient_id"),
                            rs.getString("first_name") + " " + rs.getString("last_name")});
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
            }
            return rows.toArray(new Object[0][]);
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
                    FROM patients WHERE patient_id = ? LIMIT 1
                    """;
            try (PreparedStatement st = connection.prepareStatement(sql)) {
                st.setInt(1, patientId);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) return null;
                    return new Object[]{
                            rs.getInt("patient_id"), rs.getString("first_name"), rs.getString("last_name"),
                            rs.getDate("birth_date"), rs.getString("sex"), rs.getString("contact_number"),
                            rs.getString("email"), rs.getString("address")
                    };
                }
            } catch (SQLException e) {
                Alert.fatalError(e.getMessage());
                return null;
            }
        }

        public static String updatePatient(int patientId, String firstName, String lastName, Date birthDate,
                                           String sex, String contactNumber, String email, String address) {
            String sql = """
                    UPDATE patients SET first_name=?, last_name=?, birth_date=?, sex=?,
                        contact_number=?, email=?, address=?, updated_at=CURRENT_TIMESTAMP
                    WHERE patient_id=?
                    """;
            try (PreparedStatement st = connection.prepareStatement(sql)) {
                st.setString(1, firstName);
                st.setString(2, lastName);
                st.setDate(3, birthDate);
                st.setString(4, sex);
                st.setString(5, contactNumber);
                st.setString(6, email);
                st.setString(7, address);
                st.setInt(8, patientId);
                if (st.executeUpdate() == 0) return "Failed to update patient.";
                Logs.logToDatabase(Enums.ActionType.UPDATE, Enums.Tables.PATIENTS, patientId,
                        "Updated patient: " + firstName + " " + lastName);
                return null;
            } catch (SQLException e) {
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate"))
                    return "Patient already exists.";
                Alert.fatalError(e.getMessage());
                return "Database error.";
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
    }
}