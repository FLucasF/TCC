package com.pousadamareaita.database;

import java.sql.*;

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
        try (Connection conn = getConnection()) {
            createTables(conn);
            insertInitialData(conn);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        String[] tableCreations = {
            """
            CREATE TABLE IF NOT EXISTS rooms (
                number INTEGER PRIMARY KEY,
                type VARCHAR(20) NOT NULL,
                capacity INTEGER NOT NULL,
                nightly_rate NUMERIC(10, 2) NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS reservations (
                id SERIAL PRIMARY KEY,
                room_number INTEGER NOT NULL REFERENCES rooms(number),
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(20),
                company_cnpj VARCHAR(20),
                company_name VARCHAR(255),
                guest_passport VARCHAR(50),
                guest_country VARCHAR(100),
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(20) NOT NULL,
                nights INTEGER NOT NULL,
                subtotal NUMERIC(10, 2) NOT NULL,
                discount NUMERIC(10, 2),
                total NUMERIC(10, 2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid NUMERIC(10, 2) DEFAULT 0,
                refund NUMERIC(10, 2)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                channel VARCHAR(50) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject VARCHAR(255) NOT NULL,
                body TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """
        };

        try (Statement stmt = conn.createStatement()) {
            for (String sql : tableCreations) {
                stmt.execute(sql);
            }
        }
    }

    private void insertInitialData(Connection conn) throws SQLException {
        String checkRoomsSQL = "SELECT COUNT(*) FROM rooms";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkRoomsSQL)) {
            rs.next();
            if (rs.getInt(1) == 0) {
                String insertRoomsSQL = """
                    INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                    (101, 'STANDARD', 2, 249.90),
                    (102, 'STANDARD', 2, 249.90),
                    (201, 'DELUXE', 3, 389.90),
                    (202, 'DELUXE', 3, 389.90),
                    (301, 'SUITE', 4, 579.90)
                """;
                try (Statement insertStmt = conn.createStatement()) {
                    insertStmt.execute(insertRoomsSQL);
                }
            }
        }
    }
}
