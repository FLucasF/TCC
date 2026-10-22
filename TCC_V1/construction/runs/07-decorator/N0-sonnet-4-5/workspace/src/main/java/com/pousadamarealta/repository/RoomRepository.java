package com.pousadamarealta.repository;

import com.pousadamarealta.config.Database;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    public List<Room> findAll() {
        String sql = "SELECT number, type FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Room room = new Room();
                room.setNumber(rs.getString("number"));
                room.setType(RoomType.valueOf(rs.getString("type")));
                rooms.add(room);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Room room = new Room();
                    room.setNumber(rs.getString("number"));
                    room.setType(RoomType.valueOf(rs.getString("type")));
                    return Optional.of(room);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room", e);
        }

        return Optional.empty();
    }
}
