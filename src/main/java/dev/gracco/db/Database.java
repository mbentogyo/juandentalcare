package dev.gracco.db;

import dev.gracco.ui.Alert;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    @Getter(AccessLevel.PROTECTED) private static Connection connection;

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

    public static void shutdown(){
        try {
            connection.close();
        } catch (SQLException e) {
            Alert.fatalError(e.getMessage());
        }
    }
}
