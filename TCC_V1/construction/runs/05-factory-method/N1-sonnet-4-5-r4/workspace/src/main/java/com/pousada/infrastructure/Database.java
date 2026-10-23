package com.pousada.infrastructure;

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
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void initialize() {
        String schema = """
            CREATE TABLE IF NOT EXISTS rooms (
                number VARCHAR(10) PRIMARY KEY,
                type VARCHAR(20) NOT NULL
            );

            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number VARCHAR(10) NOT NULL REFERENCES rooms(number),
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
                nightly_rate DECIMAL(10, 2) NOT NULL,
                nights INTEGER NOT NULL,
                subtotal DECIMAL(10, 2) NOT NULL,
                discount DECIMAL(10, 2) NOT NULL,
                total DECIMAL(10, 2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid DECIMAL(10, 2) NOT NULL,
                refund DECIMAL(10, 2)
            );

            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject VARCHAR(500) NOT NULL,
                body TEXT NOT NULL
            );

            INSERT INTO rooms (number, type) VALUES
                ('101', 'STANDARD'),
                ('102', 'STANDARD'),
                ('201', 'DELUXE'),
                ('202', 'DELUXE'),
                ('301', 'SUITE')
            ON CONFLICT (number) DO NOTHING;
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(schema);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}
