package com.pousada.service;

import com.pousada.database.Database;
import com.pousada.domain.Room;
import com.pousada.domain.RoomType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class RoomService {
    private static final Map<Integer, Room> rooms = new HashMap<>();

    public static void loadRooms() throws Exception {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number")) {

            while (rs.next()) {
                Room room = new Room(
                    rs.getInt("number"),
                    RoomType.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                );
                rooms.put(room.getNumber(), room);
            }
        }
    }

    public static List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public static Room getRoomByNumber(int number) {
        return rooms.get(number);
    }
}
