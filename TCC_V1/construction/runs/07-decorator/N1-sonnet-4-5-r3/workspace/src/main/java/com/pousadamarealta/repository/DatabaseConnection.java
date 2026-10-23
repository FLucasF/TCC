package com.pousadamarealta.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static HikariDataSource dataSource;

    public static void initialize(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setAutoCommit(true);

        dataSource = new HikariDataSource(config);
        createSchema();
    }

    private static void createSchema() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number VARCHAR(10) PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    capacity INT NOT NULL,
                    nightly_rate DECIMAL(10,2) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number VARCHAR(10) NOT NULL,
                    room_type VARCHAR(20) NOT NULL,
                    base_nightly_rate DECIMAL(10,2) NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(11) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INT NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0,
                    refund DECIMAL(10,2),
                    FOREIGN KEY (room_number) REFERENCES rooms(number)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject VARCHAR(500) NOT NULL,
                    body TEXT NOT NULL,
                    status VARCHAR(20) NOT NULL
                )
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database schema", e);
        }
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource not initialized");
        }
        return dataSource;
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
