package com.pousada.service;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RoomRepository {
    private final Map<Integer, Room> rooms = new HashMap<>();

    public RoomRepository() {
        rooms.put(101, new Room(101, RoomType.STANDARD));
        rooms.put(102, new Room(102, RoomType.STANDARD));
        rooms.put(201, new Room(201, RoomType.DELUXE));
        rooms.put(202, new Room(202, RoomType.DELUXE));
        rooms.put(301, new Room(301, RoomType.SUITE));
    }

    public Room getRoom(int number) {
        return rooms.get(number);
    }

    public Collection<Room> getAllRooms() {
        return rooms.values();
    }
}
