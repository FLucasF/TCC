package com.marealta.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RoomsSeeder {
    public static void seedRooms(Connection conn) throws SQLException {
        String sql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            addRoom(stmt, 101, "STANDARD", 2, new BigDecimal("249.90"));
            addRoom(stmt, 102, "STANDARD", 2, new BigDecimal("249.90"));
            addRoom(stmt, 201, "DELUXE", 3, new BigDecimal("389.90"));
            addRoom(stmt, 202, "DELUXE", 3, new BigDecimal("389.90"));
            addRoom(stmt, 301, "SUITE", 4, new BigDecimal("579.90"));
        }
    }

    private static void addRoom(PreparedStatement stmt, int number, String type, int capacity, BigDecimal rate) throws SQLException {
        stmt.setInt(1, number);
        stmt.setString(2, type);
        stmt.setInt(3, capacity);
        stmt.setBigDecimal(4, rate);
        stmt.addBatch();
        stmt.executeBatch();
    }
}
