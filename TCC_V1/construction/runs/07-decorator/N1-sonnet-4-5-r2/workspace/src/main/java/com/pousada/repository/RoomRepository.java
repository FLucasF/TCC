package com.pousada.repository;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import com.pousada.infrastructure.Database;

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

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Room room = new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type"))
                );
                rooms.add(room);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Room room = new Room(
                        rs.getString("number"),
                        RoomType.valueOf(rs.getString("type"))
                    );
                    return Optional.of(room);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }

        return Optional.empty();
    }
}
