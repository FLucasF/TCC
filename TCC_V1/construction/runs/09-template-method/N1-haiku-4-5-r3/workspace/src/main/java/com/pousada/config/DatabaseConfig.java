package com.pousada.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseConfig {
    private static HikariDataSource dataSource;

    public static void initialize() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Database environment variables not set");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);

        dataSource = new HikariDataSource(config);

        createSchema();
    }

    private static void createSchema() {
        String schema = """
            CREATE TABLE IF NOT EXISTS rooms (
                number INTEGER PRIMARY KEY,
                type VARCHAR(20) NOT NULL,
                capacity INTEGER NOT NULL,
                nightly_rate DECIMAL(10, 2) NOT NULL
            );

            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number INTEGER NOT NULL REFERENCES rooms(number),
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(20) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(20) NOT NULL,
                nights INTEGER NOT NULL,
                subtotal DECIMAL(10, 2) NOT NULL,
                discount DECIMAL(10, 2),
                total DECIMAL(10, 2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                refund DECIMAL(10, 2)
            );

            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject VARCHAR(255),
                body TEXT NOT NULL
            );

            CREATE INDEX IF NOT EXISTS idx_reservations_room ON reservations(room_number);
            CREATE INDEX IF NOT EXISTS idx_reservations_check_in ON reservations(check_in);
            CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status);
            """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : schema.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql.trim());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create database schema", e);
        }
    }

    public static HikariDataSource getDataSource() {
        if (dataSource == null) {
            initialize();
        }
        return dataSource;
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
