package com.pousada.marealta.service;

import com.pousada.marealta.db.Database;
import com.pousada.marealta.model.Room;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomService {
    public static List<Room> getAllRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             var stmt = conn.createStatement()) {

            var result = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");

            while (result.next()) {
                rooms.add(new Room(
                        result.getInt("number"),
                        result.getString("type"),
                        result.getInt("capacity"),
                        result.getBigDecimal("nightly_rate")
                ));
            }
        }

        return rooms;
    }

    public static Room getRoomById(int id) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.createStatement()) {

            var result = stmt.executeQuery(
                    "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = " + id);

            if (result.next()) {
                return new Room(
                        result.getInt("number"),
                        result.getString("type"),
                        result.getInt("capacity"),
                        result.getBigDecimal("nightly_rate")
                );
            }
        }

        return null;
    }
}
