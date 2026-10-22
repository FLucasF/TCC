package com.pousadamarealta.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static Database instance;
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static Database getInstance(String url, String user, String password) {
        if (instance == null) {
            instance = new Database(url, user, password);
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void initialize() throws SQLException {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                createTables(stmt);
                insertRooms(stmt);
            }
        }
    }

    private void createTables(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS rooms (
                number INTEGER PRIMARY KEY,
                type VARCHAR(50) NOT NULL,
                capacity INTEGER NOT NULL,
                nightly_rate DECIMAL(10, 2) NOT NULL
            )
        """);

        stmt.execute("""
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
                subtotal DECIMAL(10, 2) NOT NULL,
                discount DECIMAL(10, 2) NOT NULL,
                total DECIMAL(10, 2) NOT NULL,
                status VARCHAR(50) NOT NULL,
                amount_paid DECIMAL(10, 2) DEFAULT 0,
                refund DECIMAL(10, 2),
                FOREIGN KEY (room_number) REFERENCES rooms(number)
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                channel VARCHAR(50) NOT NULL,
                to_address VARCHAR(255) NOT NULL,
                subject VARCHAR(500) NOT NULL,
                body TEXT NOT NULL,
                status VARCHAR(50) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }

    private void insertRooms(Statement stmt) throws SQLException {
        stmt.execute("DELETE FROM rooms");
        stmt.execute("""
            INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
            (101, 'STANDARD', 2, 249.90),
            (102, 'STANDARD', 2, 249.90),
            (201, 'DELUXE', 3, 389.90),
            (202, 'DELUXE', 3, 389.90),
            (301, 'SUITE', 4, 579.90)
        """);
    }
}
