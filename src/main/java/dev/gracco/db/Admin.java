package dev.gracco.db;

import dev.gracco.ui.Alert;
import dev.gracco.util.Validation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Admin {
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

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
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

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new Object[] {
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

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
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

        try (PreparedStatement statement = Database.getConnection().prepareStatement(
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