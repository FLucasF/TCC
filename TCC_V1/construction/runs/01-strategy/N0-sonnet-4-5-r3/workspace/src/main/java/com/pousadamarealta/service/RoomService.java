package com.pousadamarealta.service;

import com.pousadamarealta.config.Database;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.exception.ApiException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomService {
    private final Database database;

    public RoomService(Database database) {
        this.database = database;
    }

    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Room room = new Room();
                room.setNumber(rs.getString("number"));
                room.setType(RoomType.valueOf(rs.getString("type")));
                rooms.add(room);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get rooms", e);
        }

        return rooms;
    }

    public Room getRoomByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Room room = new Room();
                    room.setNumber(rs.getString("number"));
                    room.setType(RoomType.valueOf(rs.getString("type")));
                    return room;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get room", e);
        }

        throw new ApiException(404, "ROOM_NOT_FOUND", "Room not found: " + number);
    }
}
