package com.pousadamarealta.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {
    public static void initialize(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS rooms (" +
                "number INTEGER PRIMARY KEY, " +
                "type VARCHAR(20) NOT NULL, " +
                "capacity INTEGER NOT NULL, " +
                "nightly_rate NUMERIC(10,2) NOT NULL" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS reservations (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "room_number INTEGER NOT NULL REFERENCES rooms(number), " +
                "room_type VARCHAR(20) NOT NULL, " +
                "guest_name VARCHAR(255) NOT NULL, " +
                "guest_email VARCHAR(255) NOT NULL, " +
                "guest_cpf VARCHAR(20) NOT NULL, " +
                "check_in DATE NOT NULL, " +
                "check_out DATE NOT NULL, " +
                "guests INTEGER NOT NULL, " +
                "rate_plan VARCHAR(20) NOT NULL, " +
                "nights INTEGER NOT NULL, " +
                "subtotal NUMERIC(10,2) NOT NULL, " +
                "discount NUMERIC(10,2) NOT NULL, " +
                "total NUMERIC(10,2) NOT NULL, " +
                "status VARCHAR(20) NOT NULL, " +
                "amount_paid NUMERIC(10,2) NOT NULL DEFAULT 0, " +
                "refund NUMERIC(10,2)" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS messages (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "channel VARCHAR(50) NOT NULL, " +
                "to_address VARCHAR(255) NOT NULL, " +
                "subject VARCHAR(255) NOT NULL, " +
                "body TEXT NOT NULL" +
                ")"
            );

            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_reservations_room_dates " +
                "ON reservations(room_number, check_in, check_out) " +
                "WHERE status != 'CANCELLED'"
            );

            insertInitialRooms(stmt);
        }
    }

    private static void insertInitialRooms(Statement stmt) throws SQLException {
        stmt.execute(
            "DELETE FROM rooms"
        );

        stmt.execute(
            "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES " +
            "(101, 'STANDARD', 2, 249.90), " +
            "(102, 'STANDARD', 2, 249.90), " +
            "(201, 'DELUXE', 3, 389.90), " +
            "(202, 'DELUXE', 3, 389.90), " +
            "(301, 'SUITE', 4, 579.90)"
        );
    }
}
