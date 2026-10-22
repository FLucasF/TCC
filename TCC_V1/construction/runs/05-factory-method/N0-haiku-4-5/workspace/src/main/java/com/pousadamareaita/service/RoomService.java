package com.pousadamareaita.service;

import com.pousadamareaita.database.Database;
import com.pousadamareaita.model.Room;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomService {
    private final Database db;

    public RoomService(Database db) {
        this.db = db;
    }

    public List<Room> getAllRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = db.getConnection()) {
            String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    rooms.add(new Room(
                        rs.getInt("number"),
                        rs.getString("type"),
                        rs.getInt("capacity"),
                        rs.getBigDecimal("nightly_rate")
                    ));
                }
            }
        }
        return rooms;
    }

    public Room getRoomByNumber(int number) throws SQLException {
        try (Connection conn = db.getConnection()) {
            String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, number);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new Room(
                            rs.getInt("number"),
                            rs.getString("type"),
                            rs.getInt("capacity"),
                            rs.getBigDecimal("nightly_rate")
                        );
                    }
                }
            }
        }
        return null;
    }
}
