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
        initializeRooms();
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private static void createSchema() {
        String roomsTable = """
            CREATE TABLE IF NOT EXISTS rooms (
                number VARCHAR(10) PRIMARY KEY,
                type VARCHAR(20) NOT NULL
            )
            """;

        String reservationsTable = """
            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number VARCHAR(10) NOT NULL,
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(20) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(20) NOT NULL,
                nights INTEGER NOT NULL,
                subtotal NUMERIC(10, 2) NOT NULL,
                discount NUMERIC(10, 2) NOT NULL,
                total NUMERIC(10, 2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
                refund NUMERIC(10, 2),
                FOREIGN KEY (room_number) REFERENCES rooms(number)
            )
            """;

        String messagesTable = """
            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject TEXT NOT NULL,
                body TEXT NOT NULL,
                status VARCHAR(20) NOT NULL
            )
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(roomsTable);
            stmt.execute(reservationsTable);
            stmt.execute(messagesTable);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create schema", e);
        }
    }

    private static void initializeRooms() {
        String insertRooms = """
            INSERT INTO rooms (number, type) VALUES
                ('101', 'STANDARD'),
                ('102', 'STANDARD'),
                ('201', 'DELUXE'),
                ('202', 'DELUXE'),
                ('301', 'SUITE')
            ON CONFLICT (number) DO NOTHING
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(insertRooms);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
