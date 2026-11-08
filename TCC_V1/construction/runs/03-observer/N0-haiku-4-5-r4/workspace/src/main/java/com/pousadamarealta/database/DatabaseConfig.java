package com.pousadamarealta.database;

import java.sql.*;

public class DatabaseConfig {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public DatabaseConfig(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public void initializeDatabase() throws SQLException {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        number INTEGER PRIMARY KEY,
                        type VARCHAR(20) NOT NULL,
                        capacity INTEGER NOT NULL,
                        nightly_rate NUMERIC(10, 2) NOT NULL
                    )
                    """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id SERIAL PRIMARY KEY,
                        room_number INTEGER NOT NULL,
                        guest_name VARCHAR(255) NOT NULL,
                        guest_email VARCHAR(255) NOT NULL,
                        guest_cpf VARCHAR(14) NOT NULL,
                        check_in DATE NOT NULL,
                        check_out DATE NOT NULL,
                        guests INTEGER NOT NULL,
                        rate_plan VARCHAR(20) NOT NULL,
                        nights INTEGER NOT NULL,
                        subtotal NUMERIC(10, 2) NOT NULL,
                        discount NUMERIC(10, 2) NOT NULL,
                        total NUMERIC(10, 2) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
                        refund NUMERIC(10, 2),
                        FOREIGN KEY (room_number) REFERENCES rooms(number)
                    )
                    """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id SERIAL PRIMARY KEY,
                        channel VARCHAR(50) NOT NULL,
                        "to" VARCHAR(255) NOT NULL,
                        subject VARCHAR(255) NOT NULL,
                        body TEXT NOT NULL
                    )
                    """);

                stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status)
                    """);

                stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_reservations_room_dates
                    ON reservations(room_number, check_in, check_out)
                    """);

                stmt.execute("""
                    CREATE SEQUENCE IF NOT EXISTS reservations_id_seq
                    """);
            }
        }
    }

    public void seedRooms() throws SQLException {
        try (Connection conn = getConnection()) {
            String checkQuery = "SELECT COUNT(*) FROM rooms";
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(checkQuery);
                rs.next();
                if (rs.getInt(1) > 0) {
                    return;
                }
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (101, 'STANDARD', 2, 249.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (102, 'STANDARD', 2, 249.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (201, 'DELUXE', 3, 389.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (202, 'DELUXE', 3, 389.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (301, 'SUITE', 4, 579.90)");
            }
        }
    }
}
