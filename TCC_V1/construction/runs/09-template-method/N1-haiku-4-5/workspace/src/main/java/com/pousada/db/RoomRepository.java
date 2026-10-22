package com.pousada.db;

import com.pousada.models.Room;
import com.pousada.models.RoomType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class RoomRepository {
    private final Database database;

    public RoomRepository(Database database) {
        this.database = database;
    }

    public List<Room> findAll() throws SQLException {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(mapToRoom(rs));
            }
        }

        return rooms;
    }

    public Optional<Room> findByNumber(int number) throws SQLException {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapToRoom(rs));
                }
            }
        }

        return Optional.empty();
    }

    private Room mapToRoom(ResultSet rs) throws SQLException {
        int number = rs.getInt("number");
        String typeStr = rs.getString("type");
        int capacity = rs.getInt("capacity");
        BigDecimal nightlyRate = rs.getBigDecimal("nightly_rate");

        RoomType type = RoomType.fromLabel(typeStr);
        return new Room(number, type, capacity, nightlyRate);
    }
}
