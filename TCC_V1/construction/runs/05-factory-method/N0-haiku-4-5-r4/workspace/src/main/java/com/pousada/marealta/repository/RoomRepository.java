package com.pousada.marealta.repository;

import com.pousada.marealta.database.Database;
import com.pousada.marealta.model.Room;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    public static Optional<Room> findById(int number) throws SQLException {
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
            stmt.setInt(1, number);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        }
    }

    public static List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (var conn = Database.getConnection();
             var stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
            while (rs.next()) {
                rooms.add(mapRow(rs));
            }
        }
        return rooms;
    }

    private static Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("number"),
            rs.getString("type"),
            rs.getInt("capacity"),
            rs.getBigDecimal("nightly_rate")
        );
    }
}
