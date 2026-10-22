package com.pousada.marealta.db;

import java.sql.*;

public class Database {
    private static final String DB_URL = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : "jdbc:postgresql://localhost:5432/marealta";
    private static final String DB_USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "postgres";
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "postgres";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void initialize() throws SQLException {
        try (Connection conn = getConnection()) {
            String[] ddl = {
                    "CREATE TABLE IF NOT EXISTS rooms (" +
                            "number INTEGER PRIMARY KEY," +
                            "type VARCHAR(20) NOT NULL," +
                            "capacity INTEGER NOT NULL," +
                            "nightly_rate NUMERIC(10,2) NOT NULL" +
                            ")",

                    "CREATE TABLE IF NOT EXISTS reservations (" +
                            "id BIGSERIAL PRIMARY KEY," +
                            "room_number INTEGER NOT NULL REFERENCES rooms(number)," +
                            "guest_name VARCHAR(255) NOT NULL," +
                            "guest_email VARCHAR(255) NOT NULL," +
                            "guest_cpf VARCHAR(11) NOT NULL," +
                            "check_in DATE NOT NULL," +
                            "check_out DATE NOT NULL," +
                            "guests INTEGER NOT NULL," +
                            "rate_plan VARCHAR(20) NOT NULL," +
                            "nights INTEGER NOT NULL," +
                            "subtotal NUMERIC(10,2) NOT NULL," +
                            "discount NUMERIC(10,2) NOT NULL," +
                            "total NUMERIC(10,2) NOT NULL," +
                            "status VARCHAR(20) NOT NULL," +
                            "amount_paid NUMERIC(10,2) NOT NULL DEFAULT 0," +
                            "refund NUMERIC(10,2)" +
                            ")",

                    "CREATE TABLE IF NOT EXISTS messages (" +
                            "id BIGSERIAL PRIMARY KEY," +
                            "channel VARCHAR(50) NOT NULL," +
                            "recipient VARCHAR(255) NOT NULL," +
                            "subject VARCHAR(500) NOT NULL," +
                            "body TEXT NOT NULL," +
                            "status VARCHAR(20) NOT NULL" +
                            ")",

                    "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (101, 'STANDARD', 2, 249.90) " +
                            "ON CONFLICT DO NOTHING",
                    "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (102, 'STANDARD', 2, 249.90) " +
                            "ON CONFLICT DO NOTHING",
                    "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (201, 'DELUXE', 3, 389.90) " +
                            "ON CONFLICT DO NOTHING",
                    "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (202, 'DELUXE', 3, 389.90) " +
                            "ON CONFLICT DO NOTHING",
                    "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (301, 'SUITE', 4, 579.90) " +
                            "ON CONFLICT DO NOTHING"
            };

            for (String sql : ddl) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
            }
        }
    }
}
