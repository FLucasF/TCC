package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

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

    public Room findByNumber(int number) {
        return rooms.get(number);
    }

    public Iterable<Room> findAll() {
        return rooms.values();
    }
}
