package com.pousada.marealta.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;

    public static void initialize() throws SQLException {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUrl.isEmpty()) {
            throw new RuntimeException("DB_URL environment variable is required");
        }
        if (dbUser == null || dbUser.isEmpty()) {
            throw new RuntimeException("DB_USER environment variable is required");
        }
        if (dbPassword == null || dbPassword.isEmpty()) {
            throw new RuntimeException("DB_PASSWORD environment variable is required");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);
        logger.info("Database connection pool initialized");

        initializeTables();
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new RuntimeException("Database not initialized. Call DatabaseConfig.initialize() first.");
        }
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }

    private static void initializeTables() throws SQLException {
        try (Connection conn = getConnection()) {
            // Create rooms table
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS rooms (" +
                "  number INTEGER PRIMARY KEY," +
                "  type VARCHAR(20) NOT NULL," +
                "  capacity INTEGER NOT NULL," +
                "  nightly_rate NUMERIC(10, 2) NOT NULL" +
                ")"
            );

            // Create reservations table
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS reservations (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  room_number INTEGER NOT NULL REFERENCES rooms(number)," +
                "  guest_name VARCHAR(255) NOT NULL," +
                "  guest_email VARCHAR(255) NOT NULL," +
                "  guest_cpf VARCHAR(20)," +
                "  company_cnpj VARCHAR(20)," +
                "  company_name VARCHAR(255)," +
                "  guest_passport VARCHAR(20)," +
                "  guest_country VARCHAR(100)," +
                "  check_in DATE NOT NULL," +
                "  check_out DATE NOT NULL," +
                "  guests INTEGER NOT NULL," +
                "  rate_plan VARCHAR(20) NOT NULL," +
                "  nights INTEGER NOT NULL," +
                "  subtotal NUMERIC(10, 2) NOT NULL," +
                "  discount NUMERIC(10, 2)," +
                "  total NUMERIC(10, 2) NOT NULL," +
                "  status VARCHAR(20) NOT NULL," +
                "  amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0," +
                "  refund NUMERIC(10, 2)," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Create messages table
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS messages (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  channel VARCHAR(20) NOT NULL," +
                "  to_address VARCHAR(255) NOT NULL," +
                "  subject VARCHAR(255) NOT NULL," +
                "  body TEXT NOT NULL," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Insert rooms if they don't exist
            conn.createStatement().execute(
                "INSERT INTO rooms (number, type, capacity, nightly_rate) " +
                "VALUES (101, 'STANDARD', 2, 249.90) " +
                "ON CONFLICT (number) DO NOTHING"
            );
            conn.createStatement().execute(
                "INSERT INTO rooms (number, type, capacity, nightly_rate) " +
                "VALUES (102, 'STANDARD', 2, 249.90) " +
                "ON CONFLICT (number) DO NOTHING"
            );
            conn.createStatement().execute(
                "INSERT INTO rooms (number, type, capacity, nightly_rate) " +
                "VALUES (201, 'DELUXE', 3, 389.90) " +
                "ON CONFLICT (number) DO NOTHING"
            );
            conn.createStatement().execute(
                "INSERT INTO rooms (number, type, capacity, nightly_rate) " +
                "VALUES (202, 'DELUXE', 3, 389.90) " +
                "ON CONFLICT (number) DO NOTHING"
            );
            conn.createStatement().execute(
                "INSERT INTO rooms (number, type, capacity, nightly_rate) " +
                "VALUES (301, 'SUITE', 4, 579.90) " +
                "ON CONFLICT (number) DO NOTHING"
            );

            logger.info("Database tables initialized successfully");
        }
    }
}
