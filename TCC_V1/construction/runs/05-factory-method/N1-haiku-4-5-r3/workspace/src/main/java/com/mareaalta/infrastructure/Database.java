package com.mareaalta.infrastructure;

import java.sql.*;

public class Database {
    private static Connection connection;
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    public static void initialize(String url, String user, String password) throws SQLException {
        dbUrl = url;
        dbUser = user;
        dbPassword = password;
        connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        createSchema();
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        }
        return connection;
    }

    private static void createSchema() throws SQLException {
        String[] schemas = {
            createRoomsTable(),
            createReservationsTable(),
            createMessagesTable()
        };

        try (Statement stmt = connection.createStatement()) {
            for (String schema : schemas) {
                stmt.execute(schema);
            }
        }
    }

    private static String createRoomsTable() {
        return """
            CREATE TABLE IF NOT EXISTS rooms (
                number INTEGER PRIMARY KEY,
                type VARCHAR(50) NOT NULL,
                capacity INTEGER NOT NULL,
                nightly_rate NUMERIC(10, 2) NOT NULL
            );
            """;
    }

    private static String createReservationsTable() {
        return """
            CREATE TABLE IF NOT EXISTS reservations (
                id SERIAL PRIMARY KEY,
                room_number INTEGER NOT NULL REFERENCES rooms(number),
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(20),
                company_cnpj VARCHAR(20),
                company_name VARCHAR(255),
                guest_passport VARCHAR(50),
                guest_country VARCHAR(100),
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(50) NOT NULL,
                nights INTEGER NOT NULL,
                subtotal NUMERIC(10, 2) NOT NULL,
                discount NUMERIC(10, 2),
                total NUMERIC(10, 2) NOT NULL,
                status VARCHAR(50) NOT NULL,
                amount_paid NUMERIC(10, 2) DEFAULT 0,
                refund NUMERIC(10, 2),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;
    }

    private static String createMessagesTable() {
        return """
            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                channel VARCHAR(50) NOT NULL,
                "to" VARCHAR(255) NOT NULL,
                subject VARCHAR(255) NOT NULL,
                body TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;
    }
}
