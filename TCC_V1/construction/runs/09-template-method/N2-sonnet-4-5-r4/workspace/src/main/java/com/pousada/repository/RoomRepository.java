package com.pousada.repository;

import com.pousada.config.Database;
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

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT number, type FROM rooms ORDER BY number")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    RoomType.valueOf(rs.getString("type"))
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }
        return rooms;
    }

    public Optional<Room> findByNumber(int number) {
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT number, type FROM rooms WHERE number = ?")) {

            stmt.setInt(1, number);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new Room(
                    rs.getInt("number"),
                    RoomType.valueOf(rs.getString("type"))
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }
        return Optional.empty();
    }
}
