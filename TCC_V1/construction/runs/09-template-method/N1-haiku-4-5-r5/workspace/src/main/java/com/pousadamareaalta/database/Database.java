package com.pousadamareaalta.database;

import java.sql.*;

public class Database {
    private static final String ROOMS_TABLE = """
        CREATE TABLE IF NOT EXISTS rooms (
            number INTEGER PRIMARY KEY,
            type VARCHAR(50) NOT NULL,
            capacity INTEGER NOT NULL,
            nightly_rate NUMERIC(10, 2) NOT NULL
        )
        """;

    private static final String RESERVATIONS_TABLE = """
        CREATE TABLE IF NOT EXISTS reservations (
            id BIGSERIAL PRIMARY KEY,
            room_number INTEGER NOT NULL REFERENCES rooms(number),
            guest_name VARCHAR(255) NOT NULL,
            guest_email VARCHAR(255) NOT NULL,
            guest_cpf VARCHAR(11) NOT NULL,
            check_in DATE NOT NULL,
            check_out DATE NOT NULL,
            guests INTEGER NOT NULL,
            rate_plan VARCHAR(50) NOT NULL,
            nights INTEGER NOT NULL,
            subtotal NUMERIC(10, 2) NOT NULL,
            discount NUMERIC(10, 2) NOT NULL,
            total NUMERIC(10, 2) NOT NULL,
            status VARCHAR(50) NOT NULL,
            amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
            refund NUMERIC(10, 2),
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
        """;

    private static final String MESSAGES_TABLE = """
        CREATE TABLE IF NOT EXISTS messages (
            id BIGSERIAL PRIMARY KEY,
            channel VARCHAR(50) NOT NULL,
            to_email VARCHAR(255) NOT NULL,
            subject VARCHAR(255) NOT NULL,
            body TEXT NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
        """;

    private static Connection connection;

    public static void initialize(String dbUrl, String dbUser, String dbPassword) throws SQLException {
        connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        createTables();
        initializeRooms();
    }

    private static void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(ROOMS_TABLE);
            stmt.execute(RESERVATIONS_TABLE);
            stmt.execute(MESSAGES_TABLE);
        }
    }

    private static void initializeRooms() throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM rooms";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            rs.next();
            if (rs.getInt(1) == 0) {
                String insertSql = """
                    INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                    (101, 'STANDARD', 2, 249.90),
                    (102, 'STANDARD', 2, 249.90),
                    (201, 'DELUXE', 3, 389.90),
                    (202, 'DELUXE', 3, 389.90),
                    (301, 'SUITE', 4, 579.90)
                    """;
                try (Statement insertStmt = connection.createStatement()) {
                    insertStmt.execute(insertSql);
                }
            }
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    public static void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
