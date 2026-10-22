package com.pousadamarealta;

import java.sql.*;

public class Database {
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    static {
        dbUrl = System.getenv("DB_URL");
        dbUser = System.getenv("DB_USER");
        dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Missing database environment variables");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public static void initialize() {
        try (Connection conn = getConnection()) {
            createSchema(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createSchema(Connection conn) throws SQLException {
        String[] statements = {
            """
            CREATE TABLE IF NOT EXISTS rooms (
                number INTEGER PRIMARY KEY,
                type VARCHAR(50) NOT NULL,
                capacity INTEGER NOT NULL,
                nightly_rate NUMERIC(10, 2) NOT NULL
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number INTEGER NOT NULL REFERENCES rooms(number),
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(14) NOT NULL,
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
            """,

            """
            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel VARCHAR(50) NOT NULL,
                to_address VARCHAR(255) NOT NULL,
                subject VARCHAR(255) NOT NULL,
                body TEXT NOT NULL
            )
            """,

            """
            INSERT INTO rooms (number, type, capacity, nightly_rate)
            VALUES
                (101, 'STANDARD', 2, 249.90),
                (102, 'STANDARD', 2, 249.90),
                (201, 'DELUXE', 3, 389.90),
                (202, 'DELUXE', 3, 389.90),
                (301, 'SUITE', 4, 579.90)
            ON CONFLICT (number) DO NOTHING
            """
        };

        for (String sql : statements) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }
}
