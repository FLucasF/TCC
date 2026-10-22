package com.pousada.marealta.config;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {
    private static Connection connection;

    public static Connection getConnection() throws Exception {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(
                Config.getDbUrl(),
                Config.getDbUser(),
                Config.getDbPassword()
            );
        }
        return connection;
    }

    public static void initialize() throws Exception {
        Connection conn = getConnection();
        DatabaseMigration.runMigrations(conn);
    }

    public static void closeConnection() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
