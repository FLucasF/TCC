package com.pousadamarealta.domain.repository;

import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.model.RoomType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoomRepository {
    private final Map<String, Room> rooms = new LinkedHashMap<>();

    public RoomRepository() {
        rooms.put("101", new Room("101", RoomType.STANDARD));
        rooms.put("102", new Room("102", RoomType.STANDARD));
        rooms.put("201", new Room("201", RoomType.DELUXE));
        rooms.put("202", new Room("202", RoomType.DELUXE));
        rooms.put("301", new Room("301", RoomType.SUITE));
    }

    public List<Room> findAll() {
        return List.copyOf(rooms.values());
    }

    public Optional<Room> findByNumber(String number) {
        return Optional.ofNullable(rooms.get(number));
    }
}
