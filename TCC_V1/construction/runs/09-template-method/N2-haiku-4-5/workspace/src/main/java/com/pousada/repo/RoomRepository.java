package com.pousada.repo;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import com.pousada.util.Database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomRepository {
    public void initializeRooms() throws Exception {
        Connection conn = Database.getConnection();

        String sql = "SELECT COUNT(*) FROM rooms";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return;
            }
        }

        Room[] rooms = new Room[]{
            new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90"))
        };

        String insertSql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
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

    public List<Room> findAll() throws Exception {
        Connection conn = Database.getConnection();
        List<Room> rooms = new ArrayList<>();

        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    RoomType.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                ));
            }
        }

        return rooms;
    }

    public Room findByNumber(int number) throws Exception {
        Connection conn = Database.getConnection();

        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, number);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Room(
                        rs.getInt("number"),
                        RoomType.valueOf(rs.getString("type")),
                        rs.getInt("capacity"),
                        rs.getBigDecimal("nightly_rate")
                    );
                }
            }
        }

        return null;
    }
}
