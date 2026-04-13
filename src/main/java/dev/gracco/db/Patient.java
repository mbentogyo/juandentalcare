package dev.gracco.db;

import dev.gracco.ui.Alert;
import dev.gracco.util.Validation;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Patient {
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

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new Object[] {
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

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
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