package com.pousadamarealta.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
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

    public static DataSource getDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("Database not initialized");
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static void createSchema() {
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
                guest_cpf VARCHAR(11) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(20) NOT NULL,
                nightly_rate DECIMAL(10, 2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                refund DECIMAL(10, 2)
            );

            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject VARCHAR(500) NOT NULL,
                body TEXT NOT NULL
            );

            CREATE INDEX IF NOT EXISTS idx_reservations_room_dates
                ON reservations(room_number, check_in, check_out);
            CREATE INDEX IF NOT EXISTS idx_reservations_status
                ON reservations(status);
            CREATE INDEX IF NOT EXISTS idx_reservations_check_in
                ON reservations(check_in);
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(schema);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database schema", e);
        }
    }
}
