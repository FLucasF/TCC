package com.pousada.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {
    private static Connection connection;

    public static void init(String dbUrl, String dbUser, String dbPassword) throws Exception {
        Class.forName("org.postgresql.Driver");
        connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        createTables();
    }

    private static void createTables() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number INT PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
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
                    guest_cpf VARCHAR(14) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INT NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    nights INT NOT NULL,
                    subtotal NUMERIC(10, 2) NOT NULL,
                    discount NUMERIC(10, 2) NOT NULL,
                    total NUMERIC(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid NUMERIC(10, 2) DEFAULT 0,
                    refund NUMERIC(10, 2),
                    FOREIGN KEY (room_number) REFERENCES rooms(number)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sent_messages (
                    id SERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    to_address VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL
                )
                """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_reservations_room_dates
                ON reservations(room_number, check_in, check_out, status)
                """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_reservations_check_in
                ON reservations(check_in)
                """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_reservations_status
                ON reservations(status)
                """);
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    public static void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }
}
