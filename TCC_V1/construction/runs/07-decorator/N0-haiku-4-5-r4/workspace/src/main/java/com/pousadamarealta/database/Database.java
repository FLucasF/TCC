package com.pousadamarealta.database;

import java.sql.*;

public class Database {
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void initialize() {
        try {
            Class.forName("org.postgresql.Driver");
            createSchema();
            insertRooms();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createSchema() throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(true);

            String createRoomsTable = """
                    CREATE TABLE IF NOT EXISTS rooms (
                        number INTEGER PRIMARY KEY,
                        type VARCHAR(50) NOT NULL,
                        capacity INTEGER NOT NULL,
                        nightly_rate NUMERIC(10, 2) NOT NULL
                    )
                    """;

            String createReservationsTable = """
                    CREATE TABLE IF NOT EXISTS reservations (
                        id SERIAL PRIMARY KEY,
                        room_number INTEGER NOT NULL REFERENCES rooms(number),
                        guest_name VARCHAR(255) NOT NULL,
                        guest_email VARCHAR(255) NOT NULL,
                        guest_cpf VARCHAR(20) NOT NULL,
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
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

            String createMessagesTable = """
                    CREATE TABLE IF NOT EXISTS messages (
                        id SERIAL PRIMARY KEY,
                        channel VARCHAR(50) NOT NULL,
                        recipient VARCHAR(255) NOT NULL,
                        subject VARCHAR(255) NOT NULL,
                        body TEXT NOT NULL,
                        status VARCHAR(50) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createRoomsTable);
                stmt.execute(createReservationsTable);
                stmt.execute(createMessagesTable);
            }
        }
    }

    private static void insertRooms() throws SQLException {
        try (Connection conn = getConnection()) {
            String checkRooms = "SELECT COUNT(*) FROM rooms";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkRooms)) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    String insertRooms = """
                            INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                            (101, 'STANDARD', 2, 249.90),
                            (102, 'STANDARD', 2, 249.90),
                            (201, 'DELUXE', 3, 389.90),
                            (202, 'DELUXE', 3, 389.90),
                            (301, 'SUITE', 4, 579.90)
                            """;
                    try (Statement insertStmt = conn.createStatement()) {
                        insertStmt.execute(insertRooms);
                    }
                }
            }
        }
    }
}
