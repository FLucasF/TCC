package com.pousada.maralta.repository;

import com.pousada.maralta.domain.Room;
import com.pousada.maralta.domain.RoomType;
import com.pousada.maralta.infrastructure.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcRoomRepository implements RoomRepository {
    @Override
    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                rooms.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find rooms", e);
        }

        return rooms;
    }

    @Override
    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
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

    private Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
            rs.getString("number"),
            RoomType.valueOf(rs.getString("type"))
        );
    }
}
