package com.pousada.service;

import com.pousada.db.Database;
import com.pousada.db.Room;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomService {
    private static final Map<Integer, Room> roomCache = new HashMap<>();
    private static boolean initialized = false;

    public static List<Room> getAllRooms() throws SQLException {
        if (!initialized) {
            loadRooms();
        }
        return new ArrayList<>(roomCache.values());
    }

    public static Room getRoomByNumber(int roomNumber) throws SQLException {
        if (!initialized) {
            loadRooms();
        }
        return roomCache.get(roomNumber);
    }

    private static void loadRooms() throws SQLException {
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT * FROM rooms ORDER BY number";
            var stmt = conn.prepareStatement(sql);
            var rs = stmt.executeQuery();

            while (rs.next()) {
                int number = rs.getInt("number");
                String type = rs.getString("type");
                int capacity = rs.getInt("capacity");
                BigDecimal nightlyRate = rs.getBigDecimal("nightly_rate");

                Room room = new Room(number, type, capacity, nightlyRate);
                roomCache.put(number, room);
            }

            initialized = true;
        }
    }
}
