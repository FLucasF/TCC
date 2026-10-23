package com.pousadamarealta.infra;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

public final class DatabaseSetup {
    private final DataSource dataSource;

    public DatabaseSetup(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initialize() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number VARCHAR(10) PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    capacity INT NOT NULL,
                    nightly_rate DECIMAL(10,2) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number VARCHAR(10) NOT NULL REFERENCES rooms(number),
                    guest_name VARCHAR(200) NOT NULL,
                    guest_email VARCHAR(200) NOT NULL,
                    guest_cpf VARCHAR(14) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INT NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    nights INT NOT NULL,
                    subtotal DECIMAL(10,2) NOT NULL,
                    discount DECIMAL(10,2) NOT NULL,
                    total DECIMAL(10,2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0,
                    refund DECIMAL(10,2)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient VARCHAR(200) NOT NULL,
                    subject TEXT NOT NULL,
                    body TEXT NOT NULL,
                    status VARCHAR(20) NOT NULL
                )
            """);

            insertDefaultRoomsIfEmpty(conn);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void insertDefaultRoomsIfEmpty(Connection conn) throws Exception {
        try (var stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("""
                    INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                    ('101', 'STANDARD', 2, 249.90),
                    ('102', 'STANDARD', 2, 249.90),
                    ('201', 'DELUXE', 3, 389.90),
                    ('202', 'DELUXE', 3, 389.90),
                    ('301', 'SUITE', 4, 579.90)
                """);
            }
        }
    }
}
