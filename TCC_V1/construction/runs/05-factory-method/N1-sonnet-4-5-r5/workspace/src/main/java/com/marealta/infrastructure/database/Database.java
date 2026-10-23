package com.marealta.infrastructure.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final Connection connection;

    public Database(String url, String user, String password) throws SQLException {
        this.connection = DriverManager.getConnection(url, user, password);
        initializeTables();
    }

    public Connection getConnection() {
        return connection;
    }

    private void initializeTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number VARCHAR(10) PRIMARY KEY,
                    type VARCHAR(20) NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number VARCHAR(10) NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(14),
                    company_cnpj VARCHAR(18),
                    company_name VARCHAR(255),
                    guest_passport VARCHAR(50),
                    guest_country VARCHAR(100),
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    nights INTEGER NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    discount DECIMAL(10, 2) NOT NULL,
                    total DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10, 2) NOT NULL,
                    refund DECIMAL(10, 2),
                    FOREIGN KEY (room_number) REFERENCES rooms(number)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject VARCHAR(500) NOT NULL,
                    body TEXT NOT NULL
                )
                """);
        }
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
