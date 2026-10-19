package com.pousada.services;

import com.pousada.models.Room;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class RoomService {
    private final Map<Integer, Room> rooms = new HashMap<>();

    public RoomService() {
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.put(101, new Room(101, "STANDARD", 2, new BigDecimal("249.90")));
        rooms.put(102, new Room(102, "STANDARD", 2, new BigDecimal("249.90")));
        rooms.put(201, new Room(201, "DELUXE", 3, new BigDecimal("389.90")));
        rooms.put(202, new Room(202, "DELUXE", 3, new BigDecimal("389.90")));
        rooms.put(301, new Room(301, "SUITE", 4, new BigDecimal("579.90")));
    }

    public Room getRoom(int number) {
        return rooms.get(number);
    }

    public Map<Integer, Room> getAllRooms() {
        return new HashMap<>(rooms);
    }

    public boolean roomExists(int number) {
        return rooms.containsKey(number);
    }
}
