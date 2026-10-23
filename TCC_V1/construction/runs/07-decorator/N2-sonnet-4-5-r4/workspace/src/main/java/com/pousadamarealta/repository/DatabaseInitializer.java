package com.pousadamarealta.repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {
    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initialize() {
        createTables();
    }

    private void createTables() {
        String sql = """
            CREATE TABLE IF NOT EXISTS rooms (
                number VARCHAR(10) PRIMARY KEY,
                type VARCHAR(20) NOT NULL
            );

            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number VARCHAR(10) NOT NULL REFERENCES rooms(number),
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(14) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(20) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid DECIMAL(10, 2) NOT NULL,
                refund DECIMAL(10, 2)
            );

            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject VARCHAR(500) NOT NULL,
                body TEXT NOT NULL,
                status VARCHAR(20) NOT NULL
            );
            """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}
