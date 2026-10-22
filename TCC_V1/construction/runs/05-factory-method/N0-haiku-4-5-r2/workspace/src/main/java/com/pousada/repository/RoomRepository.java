package com.pousada.repository;

import com.pousada.db.Database;
import com.pousada.model.Room;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final Database db;

    public RoomRepository(Database db) {
        this.db = db;
    }

    public Optional<Room> findByNumber(int number) {
        String sql = "SELECT * FROM rooms WHERE number = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, number);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Room> findAll() {
        String sql = "SELECT * FROM rooms ORDER BY number";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                rooms.add(mapRow(rs));
            }
            return rooms;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("number"),
            rs.getString("type"),
            rs.getInt("capacity"),
            rs.getBigDecimal("nightly_rate")
        );
    }
}
