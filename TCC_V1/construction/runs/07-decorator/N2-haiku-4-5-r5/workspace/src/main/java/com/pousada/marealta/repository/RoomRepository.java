package com.pousada.marealta.repository;

import com.pousada.marealta.config.DatabaseConnection;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.domain.RoomType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {
    public List<Room> getAllRooms() throws Exception {
        List<Room> rooms = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    RoomType.fromString(rs.getString("type"))
                ));
            }
        }
        return rooms;
    }

    public Room getRoomByNumber(int number) throws Exception {
        Connection conn = DatabaseConnection.getConnection();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = " + number);
            if (rs.next()) {
                return new Room(
                    rs.getInt("number"),
                    RoomType.fromString(rs.getString("type"))
                );
            }
        }
        return null;
    }
}
