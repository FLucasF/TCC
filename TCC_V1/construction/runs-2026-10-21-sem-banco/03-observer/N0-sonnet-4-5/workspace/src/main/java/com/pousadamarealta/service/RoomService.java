package com.pousadamarealta.service;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoomService {
    private final Map<Integer, Room> rooms = new HashMap<>();

    public RoomService() {
        rooms.put(101, new Room(101, RoomType.STANDARD));
        rooms.put(102, new Room(102, RoomType.STANDARD));
        rooms.put(201, new Room(201, RoomType.DELUXE));
        rooms.put(202, new Room(202, RoomType.DELUXE));
        rooms.put(301, new Room(301, RoomType.SUITE));
    }

    public List<Room> getAll() {
        return List.copyOf(rooms.values());
    }

    public Optional<Room> findByNumber(int number) {
        return Optional.ofNullable(rooms.get(number));
    }
}
