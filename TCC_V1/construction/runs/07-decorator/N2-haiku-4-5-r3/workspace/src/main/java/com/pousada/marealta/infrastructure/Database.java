package com.pousada.marealta.infrastructure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public Database(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public void initialize() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        number INTEGER PRIMARY KEY,
                        type VARCHAR(50) NOT NULL,
                        capacity INTEGER NOT NULL,
                        nightly_rate NUMERIC(10, 2) NOT NULL
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id SERIAL PRIMARY KEY,
                        room_number INTEGER NOT NULL REFERENCES rooms(number),
                        guest_name VARCHAR(255) NOT NULL,
                        guest_email VARCHAR(255) NOT NULL,
                        guest_cpf VARCHAR(11) NOT NULL,
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
                        refund NUMERIC(10, 2)
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id SERIAL PRIMARY KEY,
                        channel VARCHAR(50) NOT NULL,
                        "to" VARCHAR(255) NOT NULL,
                        subject VARCHAR(500) NOT NULL,
                        body TEXT NOT NULL,
                        status VARCHAR(50) NOT NULL
                    )
                    """);

            insertDefaultRooms(stmt);
        }
    }

    private void insertDefaultRooms(Statement stmt) throws SQLException {
        stmt.execute("""
                INSERT INTO rooms (number, type, capacity, nightly_rate)
                VALUES
                  (101, 'STANDARD', 2, 249.90),
                  (102, 'STANDARD', 2, 249.90),
                  (201, 'DELUXE', 3, 389.90),
                  (202, 'DELUXE', 3, 389.90),
                  (301, 'SUITE', 4, 579.90)
                ON CONFLICT DO NOTHING
                """);
    }
}
