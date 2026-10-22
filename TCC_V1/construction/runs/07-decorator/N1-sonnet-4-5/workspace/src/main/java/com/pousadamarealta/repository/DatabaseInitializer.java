package com.pousadamarealta.repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initialize() {
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
                        guest_cpf VARCHAR(20) NOT NULL,
                        check_in DATE NOT NULL,
                        check_out DATE NOT NULL,
                        guests INTEGER NOT NULL,
                        rate_plan VARCHAR(20) NOT NULL,
                        nights INTEGER NOT NULL,
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
                        subject TEXT NOT NULL,
                        body TEXT NOT NULL,
                        status VARCHAR(20) NOT NULL
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
}
