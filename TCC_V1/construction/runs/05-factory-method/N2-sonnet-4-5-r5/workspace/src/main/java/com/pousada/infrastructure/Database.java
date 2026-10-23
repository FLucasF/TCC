package com.pousada.infrastructure;

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
        initialize();
    }

    private void initialize() {
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
                    room_type VARCHAR(20) NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(20),
                    company_cnpj VARCHAR(20),
                    company_name VARCHAR(255),
                    guest_passport VARCHAR(50),
                    guest_country VARCHAR(100),
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
                    refund DECIMAL(10, 2)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS emails (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject TEXT NOT NULL,
                    body TEXT NOT NULL
                )
            """);

            long roomCount = 0;
            try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms")) {
                if (rs.next()) {
                    roomCount = rs.getLong(1);
                }
            }

            if (roomCount == 0) {
                stmt.execute("""
                    INSERT INTO rooms (number, type) VALUES
                    ('101', 'STANDARD'),
                    ('102', 'STANDARD'),
                    ('201', 'DELUXE'),
                    ('202', 'DELUXE'),
                    ('301', 'SUITE')
                """);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void close() {
        dataSource.close();
    }
}
