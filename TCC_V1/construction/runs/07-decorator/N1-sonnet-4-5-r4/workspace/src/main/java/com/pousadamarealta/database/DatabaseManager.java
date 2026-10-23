package com.pousadamarealta.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static HikariDataSource dataSource;

    public static void initialize(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);

        dataSource = new HikariDataSource(config);
        createTables();
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private static void createTables() {
        String reservationsTable = """
            CREATE TABLE IF NOT EXISTS reservations (
                id SERIAL PRIMARY KEY,
                room_number VARCHAR(10) NOT NULL,
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(14) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(20) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                refund DECIMAL(10, 2)
            )
        """;

        String messagesTable = """
            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject VARCHAR(500) NOT NULL,
                body TEXT NOT NULL,
                status VARCHAR(20) NOT NULL
            )
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(reservationsTable);
            stmt.execute(messagesTable);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tables", e);
        }
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
