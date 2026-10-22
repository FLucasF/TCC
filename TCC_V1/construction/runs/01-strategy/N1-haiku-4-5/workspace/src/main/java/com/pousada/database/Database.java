package com.pousada.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final HikariDataSource dataSource;

    public Database(String dbUrl, String dbUser, String dbPassword) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        this.dataSource = new HikariDataSource(config);
        initializeTables();
    }

    private void initializeTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number INTEGER PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    capacity INTEGER NOT NULL,
                    nightly_rate DECIMAL(10, 2) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id SERIAL PRIMARY KEY,
                    room_number INTEGER NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(11) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(50) NOT NULL,
                    company_code VARCHAR(50),
                    nights INTEGER NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    discount DECIMAL(10, 2) NOT NULL,
                    total DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                    refund DECIMAL(10, 2),
                    FOREIGN KEY (room_number) REFERENCES rooms(number)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id SERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    destination VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            insertInitialRooms(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void insertInitialRooms(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms");
            rs.next();
            if (rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (101, 'STANDARD', 2, 249.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (102, 'STANDARD', 2, 249.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (201, 'DELUXE', 3, 389.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (202, 'DELUXE', 3, 389.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (301, 'SUITE', 4, 579.90)");
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        dataSource.close();
    }
}
