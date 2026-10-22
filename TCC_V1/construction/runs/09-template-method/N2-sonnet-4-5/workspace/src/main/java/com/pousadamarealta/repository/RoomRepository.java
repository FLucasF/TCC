package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    public void save(Room room) {
        String sql = "INSERT INTO rooms (number, type) VALUES (?, ?) ON CONFLICT (number) DO NOTHING";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, room.getNumber());
            stmt.setString(2, room.getType().name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save room", e);
        }
    }

    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room", e);
        }
    }

    public List<Room> findAll() {
        String sql = "SELECT number, type FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(mapRow(rs));
            }
            return rooms;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list rooms", e);
        }
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        String number = rs.getString("number");
        RoomType type = RoomType.valueOf(rs.getString("type"));
        return new Room(number, type);
    }
}
