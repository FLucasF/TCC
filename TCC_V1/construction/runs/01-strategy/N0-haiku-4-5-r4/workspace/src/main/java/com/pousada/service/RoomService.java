package com.pousada.service;

import com.pousada.db.Database;
import com.pousada.entity.Room;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomService {
    public List<Room> getAllRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
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

    public Room getRoom(int number) throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = " + number
            );
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
