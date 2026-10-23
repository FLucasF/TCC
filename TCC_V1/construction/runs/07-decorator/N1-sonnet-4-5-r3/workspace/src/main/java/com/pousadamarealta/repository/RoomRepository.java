package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    public void save(Room room) {
        String sql = """
            INSERT INTO rooms (number, type, capacity, nightly_rate)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (number) DO UPDATE SET
                type = EXCLUDED.type,
                capacity = EXCLUDED.capacity,
                nightly_rate = EXCLUDED.nightly_rate
        """;

        try (Connection conn = DatabaseConnection.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, room.getNumber());
            stmt.setString(2, room.getType().name());
            stmt.setInt(3, room.getCapacity());
            stmt.setBigDecimal(4, room.getNightlyRate());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save room", e);
        }
    }

    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";

        try (Connection conn = DatabaseConnection.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                RoomType type = RoomType.valueOf(rs.getString("type"));
                return Optional.of(new Room(rs.getString("number"), type));
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room", e);
        }
    }

    public List<Room> findAll() {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                RoomType type = RoomType.valueOf(rs.getString("type"));
                rooms.add(new Room(rs.getString("number"), type));
            }

            return rooms;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to list rooms", e);
        }
    }
}
