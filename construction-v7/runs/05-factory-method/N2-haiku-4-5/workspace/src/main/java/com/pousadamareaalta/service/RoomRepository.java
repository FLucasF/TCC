package com.pousadamareaalta.service;

import com.pousadamareaalta.model.Room;

import java.math.BigDecimal;
import java.util.*;

public class RoomRepository {
    private final Map<Integer, Room> rooms = new HashMap<>();

    public RoomRepository() {
        rooms.put(101, new Room(101, Room.RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.put(102, new Room(102, Room.RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.put(201, new Room(201, Room.RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.put(202, new Room(202, Room.RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.put(301, new Room(301, Room.RoomType.SUITE, 4, new BigDecimal("579.90")));
    }

    public Room findById(int roomNumber) {
        return rooms.get(roomNumber);
    }

    public Collection<Room> findAll() {
        return rooms.values();
    }
}
