package com.pousada;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomService {
    public static List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM rooms ORDER BY number");
            while (rs.next()) {
                Room room = new Room(
                    rs.getInt("number"),
                    rs.getString("type"),
                    rs.getInt("capacity"),
                    rs.getDouble("nightly_rate")
                );
                rooms.add(room);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching rooms", e);
        }
        return rooms;
    }

    public static Room findByNumber(int roomNumber) {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM rooms WHERE number = " + roomNumber);
            if (rs.next()) {
                return new Room(
                    rs.getInt("number"),
                    rs.getString("type"),
                    rs.getInt("capacity"),
                    rs.getDouble("nightly_rate")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching room", e);
        }
        return null;
    }
}
