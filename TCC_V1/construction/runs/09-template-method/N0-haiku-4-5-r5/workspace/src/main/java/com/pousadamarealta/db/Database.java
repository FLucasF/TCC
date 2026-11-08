package com.pousadamarealta.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    public static void init() {
        dbUrl = System.getenv("DB_URL");
        dbUser = System.getenv("DB_USER");
        dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null) {
            throw new RuntimeException("DB_URL environment variable not set");
        }

        try {
            createTables();
            seedRooms();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private static void createTables() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number INT PRIMARY KEY,
                    type VARCHAR(50) NOT NULL,
                    capacity INT NOT NULL,
                    nightly_rate NUMERIC(10, 2) NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id SERIAL PRIMARY KEY,
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

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id SERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    to_address VARCHAR(255) NOT NULL,
                    subject VARCHAR(500) NOT NULL,
                    body TEXT NOT NULL
                )
                """);

            conn.commit();
        }
    }

    private static void seedRooms() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            var countResult = stmt.executeQuery("SELECT COUNT(*) FROM rooms");
            countResult.next();
            if (countResult.getInt(1) == 0) {
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (101, 'STANDARD', 2, 249.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (102, 'STANDARD', 2, 249.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (201, 'DELUXE', 3, 389.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (202, 'DELUXE', 3, 389.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (301, 'SUITE', 4, 579.90)");
            }
            conn.commit();
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}
