package dev.gracco.db;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
            //todo: do something
        }

        return true;
    }
}
