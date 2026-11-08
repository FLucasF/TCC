package com.pousadamarealta.repository;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type FROM rooms ORDER BY number")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type"))
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }
        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type FROM rooms WHERE number = ?")) {

            stmt.setString(1, number);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type"))
                ));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }
    }
}
