package com.pousadamarealta.repository;

import com.pousadamarealta.database.DatabaseManager;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RoomRepository {
    private final DatabaseManager dbManager;

    public RoomRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(Room room) {
        String sql = """
            INSERT INTO rooms (number, type, capacity, nightly_rate)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (number) DO UPDATE SET
                type = EXCLUDED.type,
                capacity = EXCLUDED.capacity,
                nightly_rate = EXCLUDED.nightly_rate
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, room.getNumber());
            stmt.setString(2, room.getType().name());
            stmt.setInt(3, room.getCapacity());
            stmt.setBigDecimal(4, room.getType().getNightlyRate());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save room", e);
        }
    }

    public List<Room> findAll() {
        String sql = "SELECT number, type FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int number = rs.getInt("number");
                RoomType type = RoomType.valueOf(rs.getString("type"));
                rooms.add(new Room(number, type));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(int number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RoomType type = RoomType.valueOf(rs.getString("type"));
                    return Optional.of(new Room(number, type));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room", e);
        }

        return Optional.empty();
    }
}
