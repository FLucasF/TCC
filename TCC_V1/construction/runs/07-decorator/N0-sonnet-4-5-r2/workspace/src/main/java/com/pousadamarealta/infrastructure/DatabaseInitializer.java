package com.pousadamarealta.infrastructure;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
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
                    nights INTEGER NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    discount DECIMAL(10, 2) NOT NULL,
                    total DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                    refund DECIMAL(10, 2)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id SERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject TEXT NOT NULL,
                    body TEXT NOT NULL,
                    status VARCHAR(20) NOT NULL
                )
                """);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}
