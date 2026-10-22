package com.mareaalta.infrastructure;

import com.mareaalta.domain.Room;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class RoomRepository {
    public static void initializeRooms() throws SQLException {
        List<Room> rooms = Arrays.asList(
            new Room(101, Room.RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(102, Room.RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(201, Room.RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(202, Room.RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(301, Room.RoomType.SUITE, 4, new BigDecimal("579.90"))
        );

        String sql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Room room : rooms) {
                pstmt.setInt(1, room.getNumber());
                pstmt.setString(2, room.getType().name());
                pstmt.setInt(3, room.getCapacity());
                pstmt.setBigDecimal(4, room.getNightlyRate());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public static Room findByNumber(int number) throws SQLException {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, number);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Room(
                    rs.getInt("number"),
                    Room.RoomType.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                );
            }
        }
        return null;
    }

    public static List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    Room.RoomType.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                ));
            }
        }
        return rooms;
    }
}
