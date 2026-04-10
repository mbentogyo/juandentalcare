package dev.gracco.db;

import dev.gracco.ui.Alert;
import lombok.Getter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return "User does not exist";
                }

                String passwordHash = resultSet.getString("password_hash");

                if (!Encryption.decrypt(password, passwordHash)) {
                    return "Incorrect password";
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
}