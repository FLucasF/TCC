package com.pousada.repository;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final Database database;

    public RoomRepository(Database database) {
        this.database = database;
    }

    public void save(Room room) {
        String sql = """
            INSERT INTO ROOMS (NUMBER, TYPE, CAPACITY, NIGHTLY_RATE)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, room.getNumber());
            stmt.setString(2, room.getType().name());
            stmt.setInt(3, room.getCapacity());
            stmt.setBigDecimal(4, room.getNightlyRate());
            stmt.executeUpdate();

        } catch (SQLException e) {
            // Ignore if room already exists
            if (!e.getMessage().contains("Unique index or primary key violation")) {
                throw new RuntimeException("Failed to save room", e);
            }
        }
    }

    public List<Room> findAll() {
        String sql = "SELECT NUMBER, TYPE FROM ROOMS ORDER BY NUMBER";
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String number = rs.getString("NUMBER");
                RoomType type = RoomType.valueOf(rs.getString("TYPE"));
                rooms.add(new Room(number, type));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT NUMBER, TYPE FROM ROOMS WHERE NUMBER = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RoomType type = RoomType.valueOf(rs.getString("TYPE"));
                    return Optional.of(new Room(number, type));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room", e);
        }

        return Optional.empty();
    }
}
