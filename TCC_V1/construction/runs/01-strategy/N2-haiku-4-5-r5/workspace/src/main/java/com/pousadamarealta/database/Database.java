package com.pousadamarealta.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    public static void initialize(String url, String user, String password) {
        dbUrl = url;
        dbUser = user;
        dbPassword = password;

        try {
            Class.forName("org.postgresql.Driver");
            createSchema();
            initializeData();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    private static void createSchema() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number INTEGER PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    capacity INTEGER NOT NULL,
                    nightly_rate NUMERIC(10,2) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number INTEGER NOT NULL REFERENCES rooms(number),
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(14) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    company_code VARCHAR(20),
                    nights INTEGER NOT NULL,
                    subtotal NUMERIC(10,2) NOT NULL,
                    discount NUMERIC(10,2) NOT NULL,
                    total NUMERIC(10,2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid NUMERIC(10,2) NOT NULL DEFAULT 0,
                    refund NUMERIC(10,2)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    "to" VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }
    }

    private static void initializeData() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DELETE FROM rooms");

            stmt.execute("""
                INSERT INTO rooms (number, type, capacity, nightly_rate)
                VALUES
                    (101, 'STANDARD', 2, 249.90),
                    (102, 'STANDARD', 2, 249.90),
                    (201, 'DELUXE', 3, 389.90),
                    (202, 'DELUXE', 3, 389.90),
                    (301, 'SUITE', 4, 579.90)
            """);
        }
    }
}
