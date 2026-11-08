package com.pousadamarealta.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static Connection connection;

    public static void initialize(String dbUrl, String dbUser, String dbPassword) throws SQLException {
        connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        createTables();
        insertRooms();
    }

    public static Connection getConnection() {
        return connection;
    }

    private static void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
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
                    nights INTEGER NOT NULL,
                    subtotal NUMERIC(10,2) NOT NULL,
                    discount NUMERIC(10,2) NOT NULL,
                    total NUMERIC(10,2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid NUMERIC(10,2) NOT NULL,
                    refund NUMERIC(10,2)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    to_address VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_reservations_room_dates
                ON reservations(room_number, check_in, check_out)
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_reservations_check_in
                ON reservations(check_in)
            """);
        }
    }

    private static void insertRooms() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM rooms");
            stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (101, 'STANDARD', 2, 249.90)");
            stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (102, 'STANDARD', 2, 249.90)");
            stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (201, 'DELUXE', 3, 389.90)");
            stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (202, 'DELUXE', 3, 389.90)");
            stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (301, 'SUITE', 4, 579.90)");
        }
    }
}
