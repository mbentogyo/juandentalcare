package dev.gracco.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Logs {
    public static void logToDatabase(Enums.ActionType actionType, String tableName, Integer recordId, String description) {
        String sql = """
                INSERT INTO activity_logs (user_id, action_type, table_name, record_id, action_description)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setInt(1, User.getUserId());
            statement.setString(2, actionType.name());
            statement.setString(3, tableName);

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
                LIMIT 15 OFFSET ?
                """;

        List<Object[]> rows = new ArrayList<>();

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setInt(1, page * 15);

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
                            resultSet.getTimestamp("action_timestamp")
                    });
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch logs: " + e.getMessage(), e);
        }

        return rows.toArray(new Object[0][]);
    }
}