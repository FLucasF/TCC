package com.pousadamarealta.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private final HikariDataSource dataSource;

    public Database(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        this.dataSource = new HikariDataSource(config);
        initializeSchema();
    }

    private void initializeSchema() {
        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number INT PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    capacity INT NOT NULL,
                    nightly_rate NUMERIC(10, 2) NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id SERIAL PRIMARY KEY,
                    room_number INT NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(20) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INT NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    nights INT NOT NULL,
                    subtotal NUMERIC(10, 2) NOT NULL,
                    discount NUMERIC(10, 2) NOT NULL,
                    total NUMERIC(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
                    refund NUMERIC(10, 2),
                    FOREIGN KEY (room_number) REFERENCES rooms(number)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id SERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    "to" VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL,
                    status VARCHAR(20) NOT NULL
                )
                """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_reservations_room_dates
                ON reservations(room_number, check_in, check_out)
                WHERE status != 'CANCELLED'
                """);

            // Insert default rooms if they don't exist
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    insertDefaultRooms();
                }
            }

            stmt.close();
        } catch (SQLException e) {
            logger.error("Failed to initialize schema", e);
            throw new RuntimeException(e);
        }
    }

    private void insertDefaultRooms() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, 101);
                stmt.setString(2, "STANDARD");
                stmt.setInt(3, 2);
                stmt.setBigDecimal(4, new java.math.BigDecimal("249.90"));
                stmt.executeUpdate();

                stmt.setInt(1, 102);
                stmt.setString(2, "STANDARD");
                stmt.setInt(3, 2);
                stmt.setBigDecimal(4, new java.math.BigDecimal("249.90"));
                stmt.executeUpdate();

                stmt.setInt(1, 201);
                stmt.setString(2, "DELUXE");
                stmt.setInt(3, 3);
                stmt.setBigDecimal(4, new java.math.BigDecimal("389.90"));
                stmt.executeUpdate();

                stmt.setInt(1, 202);
                stmt.setString(2, "DELUXE");
                stmt.setInt(3, 3);
                stmt.setBigDecimal(4, new java.math.BigDecimal("389.90"));
                stmt.executeUpdate();

                stmt.setInt(1, 301);
                stmt.setString(2, "SUITE");
                stmt.setInt(3, 4);
                stmt.setBigDecimal(4, new java.math.BigDecimal("579.90"));
                stmt.executeUpdate();
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
