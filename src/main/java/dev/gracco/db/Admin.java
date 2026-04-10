package dev.gracco.db;

import dev.gracco.ui.Alert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Admin {
    public static Object[][] getAdmins() {
        String sql = """
                SELECT user_id, username, changed_pass, role_id, first_name, last_name, email, contact_number, is_active, created_at, updated_at
                FROM users
                WHERE role_id = 'Admin'
                ORDER BY user_id ASC
                """;

        List<Object[]> rows = new ArrayList<>();

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(new Object[] {
                        resultSet.getInt("user_id"),
                        resultSet.getString("username"),
                        resultSet.getBoolean("changed_pass"),
                        resultSet.getString("role_id"),
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getString("email"),
                        resultSet.getString("contact_number"),
                        resultSet.getBoolean("is_active"),
                        resultSet.getTimestamp("created_at"),
                        resultSet.getTimestamp("updated_at")
                });
            }

            return rows.toArray(new Object[0][]);

        } catch (SQLException e) {
            Alert.fatalError(e.getMessage());
            return new Object[0][0];
        }
    }
}