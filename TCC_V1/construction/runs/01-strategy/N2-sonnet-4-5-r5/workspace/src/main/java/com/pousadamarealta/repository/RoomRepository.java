package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    private final DatabaseManager dbManager;

    public RoomRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT number, type FROM rooms ORDER BY number")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String number = rs.getString("number");
                RoomType type = RoomType.valueOf(rs.getString("type"));
                rooms.add(new Room(number, type));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT number, type FROM rooms WHERE number = ?")) {

            stmt.setString(1, number);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                RoomType type = RoomType.valueOf(rs.getString("type"));
                return Optional.of(new Room(number, type));
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }
    }
}
