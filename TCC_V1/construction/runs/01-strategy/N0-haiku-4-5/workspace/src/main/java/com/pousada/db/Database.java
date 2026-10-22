package com.pousada.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class Database {
    private static HikariDataSource dataSource;

    public static void initialize() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("DB_URL, DB_USER, and DB_PASSWORD environment variables are required");
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
        try (Connection conn = dataSource.getConnection()) {
            String sql = """
                CREATE TABLE IF NOT EXISTS rooms (
                    id SERIAL PRIMARY KEY,
                    number INTEGER NOT NULL UNIQUE,
                    type VARCHAR(50) NOT NULL,
                    capacity INTEGER NOT NULL,
                    nightly_rate DECIMAL(10, 2) NOT NULL
                );

                CREATE TABLE IF NOT EXISTS reservations (
                    id SERIAL PRIMARY KEY,
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
                    subtotal DECIMAL(10, 2) NOT NULL,
                    discount DECIMAL(10, 2) NOT NULL,
                    total DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                    refund DECIMAL(10, 2),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );

                CREATE TABLE IF NOT EXISTS messages (
                    id SERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL,
                    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );

                CREATE INDEX IF NOT EXISTS idx_reservations_room_dates ON reservations(room_number, check_in, check_out);
                CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status);
                CREATE INDEX IF NOT EXISTS idx_reservations_check_in ON reservations(check_in);
                """;

            conn.createStatement().execute(sql);

            initializeRooms();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create schema", e);
        }
    }

    private static void initializeRooms() {
        try (Connection conn = dataSource.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM rooms";
            var result = conn.createStatement().executeQuery(checkSql);
            result.next();
            if (result.getInt(1) > 0) {
                return;
            }

            String insertSql = """
                INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                (101, 'STANDARD', 2, 249.90),
                (102, 'STANDARD', 2, 249.90),
                (201, 'DELUXE', 3, 389.90),
                (202, 'DELUXE', 3, 389.90),
                (301, 'SUITE', 4, 579.90)
                """;
            conn.createStatement().execute(insertSql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
