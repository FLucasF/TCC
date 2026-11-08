package com.pousada.infrastructure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void initialize() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create rooms table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number INT PRIMARY KEY,
                    type VARCHAR(20) NOT NULL
                )
            """);

            // Create reservations table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number INT NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(20) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INT NOT NULL,
                    rate_plan VARCHAR(50) NOT NULL,
                    nights INT NOT NULL,
                    subtotal NUMERIC(10, 2) NOT NULL,
                    discount NUMERIC(10, 2) NOT NULL,
                    total NUMERIC(10, 2) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
                    refund NUMERIC(10, 2),
                    FOREIGN KEY (room_number) REFERENCES rooms(number)
                )
            """);

            // Create messages table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL
                )
            """);

            // Insert rooms
            stmt.execute("DELETE FROM rooms");
            stmt.execute("INSERT INTO rooms (number, type) VALUES (101, 'STANDARD')");
            stmt.execute("INSERT INTO rooms (number, type) VALUES (102, 'STANDARD')");
            stmt.execute("INSERT INTO rooms (number, type) VALUES (201, 'DELUXE')");
            stmt.execute("INSERT INTO rooms (number, type) VALUES (202, 'DELUXE')");
            stmt.execute("INSERT INTO rooms (number, type) VALUES (301, 'SUITE')");

            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}
