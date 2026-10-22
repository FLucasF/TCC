package com.pousadamareaalta.repositories;

import com.pousadamareaalta.Database;
import com.pousadamareaalta.models.Room;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {
    public Room findByNumber(int number) throws SQLException {
        String sql = "SELECT * FROM rooms WHERE number = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }

        return null;
    }

    public List<Room> findAll() throws SQLException {
        String sql = "SELECT * FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                rooms.add(mapResultSet(rs));
            }
        }

        return rooms;
    }

    private Room mapResultSet(ResultSet rs) throws SQLException {
        return new Room(
                rs.getInt("number"),
                rs.getString("type"),
                rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate").doubleValue()
        );
    }
}
