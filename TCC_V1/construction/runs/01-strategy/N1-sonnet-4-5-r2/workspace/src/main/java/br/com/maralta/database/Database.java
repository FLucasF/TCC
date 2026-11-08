package br.com.maralta.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            String url = System.getenv("DB_URL");
            String user = System.getenv("DB_USER");
            String password = System.getenv("DB_PASSWORD");

            try {
                connection = DriverManager.getConnection(url, user, password);
                DatabaseInitializer.initialize(connection);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to connect to database", e);
            }
        }

        return connection;
    }

    public static void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
            }
        }
    }
}
