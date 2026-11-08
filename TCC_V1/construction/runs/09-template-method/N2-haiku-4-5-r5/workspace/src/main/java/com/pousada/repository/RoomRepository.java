package com.pousada.repository;

import com.pousada.database.Database;
import com.pousada.domain.Room;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class RoomRepository {
    private static final Map<Integer, Room> ROOMS = new LinkedHashMap<>();

    static {
        ROOMS.put(101, new Room(101, "STANDARD", 2, new BigDecimal("249.90")));
        ROOMS.put(102, new Room(102, "STANDARD", 2, new BigDecimal("249.90")));
        ROOMS.put(201, new Room(201, "DELUXE", 3, new BigDecimal("389.90")));
        ROOMS.put(202, new Room(202, "DELUXE", 3, new BigDecimal("389.90")));
        ROOMS.put(301, new Room(301, "SUITE", 4, new BigDecimal("579.90")));
    }

    public static void initializeRooms() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            for (Room room : ROOMS.values()) {
                String sql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (" +
                        room.number + ", '" + room.type + "', " + room.capacity + ", " + room.nightlyRate + ") " +
                        "ON CONFLICT (number) DO NOTHING";
                stmt.execute(sql);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }
    }

    public static Room findByNumber(int number) {
        return ROOMS.get(number);
    }

    public static List<Room> findAll() {
        return new ArrayList<>(ROOMS.values());
    }
}
