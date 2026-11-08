package com.marealata.database;

import java.sql.*;

public class Database {
    private static Connection connection;

    public static void initialize() throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Missing database environment variables");
        }

        connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        createTables();
        initializeRooms();
    }

    private static void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number INTEGER PRIMARY KEY,
                    type VARCHAR(50) NOT NULL,
                    capacity INTEGER NOT NULL,
                    nightly_rate NUMERIC(10,2) NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number INTEGER NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(20) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(50) NOT NULL,
                    nights INTEGER NOT NULL,
                    subtotal NUMERIC(10,2) NOT NULL,
                    discount NUMERIC(10,2) NOT NULL,
                    total NUMERIC(10,2) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    amount_paid NUMERIC(10,2) NOT NULL DEFAULT 0,
                    refund NUMERIC(10,2),
                    FOREIGN KEY (room_number) REFERENCES rooms(number)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    recipient_email VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL
                )
                """);
        }
    }

    private static void initializeRooms() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (number) DO NOTHING")) {

            stmt.setInt(1, 101);
            stmt.setString(2, "STANDARD");
            stmt.setInt(3, 2);
            stmt.setBigDecimal(4, new java.math.BigDecimal("249.90"));
            stmt.addBatch();

            stmt.setInt(1, 102);
            stmt.setString(2, "STANDARD");
            stmt.setInt(3, 2);
            stmt.setBigDecimal(4, new java.math.BigDecimal("249.90"));
            stmt.addBatch();

            stmt.setInt(1, 201);
            stmt.setString(2, "DELUXE");
            stmt.setInt(3, 3);
            stmt.setBigDecimal(4, new java.math.BigDecimal("389.90"));
            stmt.addBatch();

            stmt.setInt(1, 202);
            stmt.setString(2, "DELUXE");
            stmt.setInt(3, 3);
            stmt.setBigDecimal(4, new java.math.BigDecimal("389.90"));
            stmt.addBatch();

            stmt.setInt(1, 301);
            stmt.setString(2, "SUITE");
            stmt.setInt(3, 4);
            stmt.setBigDecimal(4, new java.math.BigDecimal("579.90"));
            stmt.addBatch();

            stmt.executeBatch();
        }
    }

    public static Connection getConnection() {
        return connection;
    }
}
