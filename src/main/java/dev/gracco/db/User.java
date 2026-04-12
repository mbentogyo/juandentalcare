package dev.gracco.db;

import dev.gracco.ui.Alert;
import lombok.Getter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class User {
    @Getter private static int userId;
    @Getter private static String username;
    @Getter private static String firstName;
    @Getter private static String lastName;
    @Getter private static boolean changedPassword;
    @Getter private static Enums.Role role;

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

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, usernameInput);

            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return "Incorrect username or password.";

                String passwordHash = resultSet.getString("password_hash");

                if (!Encryption.decrypt(password, passwordHash)) return "Incorrect username or password.";
                if (!resultSet.getBoolean("is_active")) return "User is set as inactive. Contact your administrator";

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

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
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