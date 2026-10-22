package com.pousada.marealta.infrastructure.persistence;

import java.sql.*;

public class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        initializeTables();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void initializeTables() {
        try (Connection conn = getConnection()) {
            createRoomsTable(conn);
            createReservationsTable(conn);
            createMessagesTable(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createRoomsTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS rooms (
                number INTEGER PRIMARY KEY,
                type VARCHAR(50) NOT NULL
            )
            """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }

        String insert = "INSERT INTO rooms (number, type) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement stmt = conn.prepareStatement(insert)) {
            stmt.setInt(1, 101);
            stmt.setString(2, "STANDARD");
            stmt.addBatch();

            stmt.setInt(1, 102);
            stmt.setString(2, "STANDARD");
            stmt.addBatch();

            stmt.setInt(1, 201);
            stmt.setString(2, "DELUXE");
            stmt.addBatch();

            stmt.setInt(1, 202);
            stmt.setString(2, "DELUXE");
            stmt.addBatch();

            stmt.setInt(1, 301);
            stmt.setString(2, "SUITE");
            stmt.addBatch();

            stmt.executeBatch();
        }
    }

    private void createReservationsTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS reservations (
                id SERIAL PRIMARY KEY,
                room_number INTEGER NOT NULL,
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
                FOREIGN KEY (room_number) REFERENCES rooms(number)
            )
            """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void createMessagesTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                channel VARCHAR(50) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject VARCHAR(255) NOT NULL,
                body TEXT NOT NULL,
                status VARCHAR(50) NOT NULL
            )
            """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
