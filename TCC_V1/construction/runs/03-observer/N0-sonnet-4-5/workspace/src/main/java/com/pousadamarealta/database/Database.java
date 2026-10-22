package com.pousadamarealta.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final HikariDataSource dataSource;

    public Database(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setAutoCommit(true);

        this.dataSource = new HikariDataSource(config);
        initializeSchema();
    }

    private void initializeSchema() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        number VARCHAR(10) PRIMARY KEY,
                        type VARCHAR(20) NOT NULL,
                        capacity INT NOT NULL,
                        nightly_rate DECIMAL(10, 2) NOT NULL
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id SERIAL PRIMARY KEY,
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
                        amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                        refund DECIMAL(10, 2),
                        FOREIGN KEY (room_number) REFERENCES rooms(number)
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id SERIAL PRIMARY KEY,
                        channel VARCHAR(20) NOT NULL,
                        recipient VARCHAR(255) NOT NULL,
                        subject TEXT NOT NULL,
                        body TEXT NOT NULL
                    )
                    """);

            long count = 0;
            try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms")) {
                if (rs.next()) {
                    count = rs.getLong(1);
                }
            }

            if (count == 0) {
                stmt.execute("""
                        INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                        ('101', 'STANDARD', 2, 249.90),
                        ('102', 'STANDARD', 2, 249.90),
                        ('201', 'DELUXE', 3, 389.90),
                        ('202', 'DELUXE', 3, 389.90),
                        ('301', 'SUITE', 4, 579.90)
                        """);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
