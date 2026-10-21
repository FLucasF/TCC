package com.pousamarealta.services;

import com.pousamarealta.models.Room;
import java.math.BigDecimal;
import java.util.*;

public class RoomService {
    private Map<Integer, Room> rooms;

    public RoomService() {
        this.rooms = new HashMap<>();
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.put(101, new Room(101, "STANDARD", 2, new BigDecimal("249.90")));
        rooms.put(102, new Room(102, "STANDARD", 2, new BigDecimal("249.90")));
        rooms.put(201, new Room(201, "DELUXE", 3, new BigDecimal("389.90")));
        rooms.put(202, new Room(202, "DELUXE", 3, new BigDecimal("389.90")));
        rooms.put(301, new Room(301, "SUITE", 4, new BigDecimal("579.90")));
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Room getRoomByNumber(int number) {
        return rooms.get(number);
    }
}
