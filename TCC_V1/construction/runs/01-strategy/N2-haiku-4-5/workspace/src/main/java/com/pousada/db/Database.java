package com.pousada.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class Database {
    private static final Logger log = LoggerFactory.getLogger(Database.class);
    private static HikariDataSource dataSource;

    public static synchronized void initialize() throws SQLException {
        if (dataSource != null) {
            return;
        }

        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Database configuration missing: DB_URL, DB_USER, DB_PASSWORD");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        dataSource = new HikariDataSource(config);
        log.info("Database connection pool initialized");

        initializeSchema();
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("Database not initialized");
        }
        return dataSource.getConnection();
    }

    private static void initializeSchema() {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                String schema = """
                    CREATE TABLE IF NOT EXISTS rooms (
                        number INT PRIMARY KEY,
                        type VARCHAR(50) NOT NULL,
                        capacity INT NOT NULL,
                        nightly_rate NUMERIC(10,2) NOT NULL
                    );

                    CREATE TABLE IF NOT EXISTS reservations (
                        id BIGSERIAL PRIMARY KEY,
                        room_number INT NOT NULL,
                        guest_name VARCHAR(255) NOT NULL,
                        guest_email VARCHAR(255) NOT NULL,
                        guest_cpf VARCHAR(20) NOT NULL,
                        check_in DATE NOT NULL,
                        check_out DATE NOT NULL,
                        guests INT NOT NULL,
                        rate_plan VARCHAR(50) NOT NULL,
                        company_code VARCHAR(50),
                        nights INT NOT NULL,
                        subtotal NUMERIC(10,2) NOT NULL,
                        discount NUMERIC(10,2) NOT NULL,
                        total NUMERIC(10,2) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        amount_paid NUMERIC(10,2) NOT NULL DEFAULT 0,
                        refund NUMERIC(10,2),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (room_number) REFERENCES rooms(number)
                    );

                    CREATE TABLE IF NOT EXISTS messages (
                        id BIGSERIAL PRIMARY KEY,
                        channel VARCHAR(50) NOT NULL,
                        to_address VARCHAR(255) NOT NULL,
                        subject VARCHAR(255) NOT NULL,
                        body TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );

                    CREATE INDEX IF NOT EXISTS idx_reservations_room_status
                        ON reservations(room_number, status);
                    CREATE INDEX IF NOT EXISTS idx_reservations_check_in_out
                        ON reservations(check_in, check_out);
                    """;

                for (String statement : schema.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        conn.createStatement().execute(trimmed);
                    }
                }

                // Insert rooms if they don't exist
                conn.createStatement().execute(
                    "INSERT INTO rooms VALUES (101, 'STANDARD', 2, 249.90) ON CONFLICT DO NOTHING"
                );
                conn.createStatement().execute(
                    "INSERT INTO rooms VALUES (102, 'STANDARD', 2, 249.90) ON CONFLICT DO NOTHING"
                );
                conn.createStatement().execute(
                    "INSERT INTO rooms VALUES (201, 'DELUXE', 3, 389.90) ON CONFLICT DO NOTHING"
                );
                conn.createStatement().execute(
                    "INSERT INTO rooms VALUES (202, 'DELUXE', 3, 389.90) ON CONFLICT DO NOTHING"
                );
                conn.createStatement().execute(
                    "INSERT INTO rooms VALUES (301, 'SUITE', 4, 579.90) ON CONFLICT DO NOTHING"
                );

                conn.commit();
                log.info("Database schema initialized");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("Failed to initialize database schema", e);
            throw new RuntimeException(e);
        }
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
