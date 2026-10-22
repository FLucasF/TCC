package com.pousadamarealta.infrastructure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private final HikariDataSource dataSource;

    public Database(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        this.dataSource = new HikariDataSource(config);
        initializeSchema();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void close() {
        dataSource.close();
    }

    private void initializeSchema() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number VARCHAR(10) PRIMARY KEY,
                    type VARCHAR(20) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number VARCHAR(10) NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(14) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INT NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    nights INT NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    discount DECIMAL(10, 2) NOT NULL,
                    total DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10, 2) NOT NULL,
                    refund DECIMAL(10, 2),
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

            insertRoomsIfEmpty(conn);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    private void insertRoomsIfEmpty(Connection conn) throws SQLException {
        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms")) {
            if (rs.next() && rs.getInt(1) == 0) {
                try (var insert = conn.prepareStatement(
                    "INSERT INTO rooms (number, type) VALUES (?, ?)")) {

                    insertRoom(insert, "101", "STANDARD");
                    insertRoom(insert, "102", "STANDARD");
                    insertRoom(insert, "201", "DELUXE");
                    insertRoom(insert, "202", "DELUXE");
                    insertRoom(insert, "301", "SUITE");
                }
            }
        }
    }

    private void insertRoom(java.sql.PreparedStatement stmt, String number, String type) throws SQLException {
        stmt.setString(1, number);
        stmt.setString(2, type);
        stmt.executeUpdate();
    }
}
