package com.marealata.dao;

import com.marealata.database.Database;
import com.marealata.model.Room;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class RoomDao {
    public static List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";

        try (Statement stmt = Database.getConnection().createStatement();
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
        return rooms;
    }

    public static Room findByNumber(int number) throws SQLException {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";

        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
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
        return null;
    }
}
