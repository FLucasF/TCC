package com.pousadamareaalta.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        initSchema();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void initSchema() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number INTEGER NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(14) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(50) NOT NULL,
                    company_code VARCHAR(50),
                    nights INTEGER NOT NULL,
                    subtotal NUMERIC(10, 2) NOT NULL,
                    discount NUMERIC(10, 2) NOT NULL,
                    total NUMERIC(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
                    refund NUMERIC(10, 2)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject VARCHAR(500),
                    body TEXT NOT NULL
                )
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
}
