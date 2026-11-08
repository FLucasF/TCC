package com.pousada;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static HikariDataSource dataSource;

    public static void init() {
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

        createTables();
        initializeRooms();
    }

    private static void createTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number INTEGER PRIMARY KEY,
                    type VARCHAR(50) NOT NULL,
                    capacity INTEGER NOT NULL,
                    nightly_rate NUMERIC(10, 2) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id SERIAL PRIMARY KEY,
                    room_number INTEGER NOT NULL REFERENCES rooms(number),
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(20) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(50) NOT NULL,
                    nights INTEGER NOT NULL,
                    subtotal NUMERIC(10, 2) NOT NULL,
                    discount NUMERIC(10, 2) NOT NULL,
                    total NUMERIC(10, 2) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
                    refund NUMERIC(10, 2)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id SERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    "to" VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tables", e);
        }
    }

    private static void initializeRooms() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            var rs = stmt.executeQuery("SELECT COUNT(*) as count FROM rooms");
            rs.next();
            if (rs.getInt("count") == 0) {
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (101, 'STANDARD', 2, 249.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (102, 'STANDARD', 2, 249.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (201, 'DELUXE', 3, 389.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (202, 'DELUXE', 3, 389.90)");
                stmt.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (301, 'SUITE', 4, 579.90)");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize rooms", e);
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
