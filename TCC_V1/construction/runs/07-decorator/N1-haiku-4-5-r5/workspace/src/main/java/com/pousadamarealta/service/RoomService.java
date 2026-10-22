package com.pousadamarealta.service;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.model.Room;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class RoomService {
    public static List<Room> getAllRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            var rs = conn.createStatement().executeQuery(
                "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");

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

    public static Room getRoom(int number) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?");
            stmt.setInt(1, number);
            var rs = stmt.executeQuery();

            if (rs.next()) {
                return new Room(
                    rs.getInt("number"),
                    rs.getString("type"),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                );
            }
        }
        return null;
    }
}
