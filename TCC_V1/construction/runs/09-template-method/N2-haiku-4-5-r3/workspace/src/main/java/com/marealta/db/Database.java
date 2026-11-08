package com.marealta.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private static HikariDataSource dataSource;

    public static void initialize(String dbUrl, String dbUser, String dbPassword) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        dataSource = new HikariDataSource(config);
        logger.info("Database connection pool initialized");

        createSchema();
    }

    private static void createSchema() {
        String schema = """
            CREATE TABLE IF NOT EXISTS rooms (
                number INTEGER PRIMARY KEY,
                type VARCHAR(50) NOT NULL,
                capacity INTEGER NOT NULL,
                nightly_rate NUMERIC(10,2) NOT NULL
            );

            CREATE TABLE IF NOT EXISTS reservations (
                id SERIAL PRIMARY KEY,
                room_number INTEGER NOT NULL REFERENCES rooms(number),
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(14) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(50) NOT NULL,
                nights INTEGER NOT NULL,
                subtotal NUMERIC(10,2) NOT NULL,
                discount NUMERIC(10,2) NOT NULL,
                total NUMERIC(10,2) NOT NULL,
                status VARCHAR(50) NOT NULL,
                amount_paid NUMERIC(10,2) NOT NULL DEFAULT 0,
                refund NUMERIC(10,2)
            );

            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                channel VARCHAR(50) NOT NULL,
                recipient_email VARCHAR(255) NOT NULL,
                subject VARCHAR(255) NOT NULL,
                body TEXT NOT NULL
            );

            INSERT INTO rooms (number, type, capacity, nightly_rate)
            SELECT 101, 'STANDARD', 2, 249.90
            WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 101);

            INSERT INTO rooms (number, type, capacity, nightly_rate)
            SELECT 102, 'STANDARD', 2, 249.90
            WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 102);

            INSERT INTO rooms (number, type, capacity, nightly_rate)
            SELECT 201, 'DELUXE', 3, 389.90
            WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 201);

            INSERT INTO rooms (number, type, capacity, nightly_rate)
            SELECT 202, 'DELUXE', 3, 389.90
            WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 202);

            INSERT INTO rooms (number, type, capacity, nightly_rate)
            SELECT 301, 'SUITE', 4, 579.90
            WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 301);
            """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : schema.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmt.executeUpdate(sql);
                }
            }
            logger.info("Database schema created successfully");
        } catch (SQLException e) {
            logger.error("Error creating database schema", e);
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
