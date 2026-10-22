package com.pousadamarealta.repository;

import com.pousadamarealta.config.Database;
import com.pousadamarealta.domain.enums.RoomType;
import com.pousadamarealta.domain.model.Room;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    public void save(Room room) {
        String sql = """
            INSERT INTO rooms (number, type)
            VALUES (?, ?)
            ON CONFLICT (number) DO UPDATE SET type = EXCLUDED.type
            """;

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
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room", e);
        }

        return Optional.empty();
    }

    public List<Room> findAll() {
        String sql = "SELECT number, type FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                rooms.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list rooms", e);
        }

        return rooms;
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
            rs.getString("number"),
            RoomType.valueOf(rs.getString("type"))
        );
    }
}
