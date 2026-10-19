package com.pousadamarealta.service;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoomService {
    private final Map<String, Room> rooms = new HashMap<>();

    public RoomService() {
        rooms.put("101", new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.put("102", new Room("102", RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.put("201", new Room("201", RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.put("202", new Room("202", RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.put("301", new Room("301", RoomType.SUITE, 4, new BigDecimal("579.90")));
    }

    public List<Room> getAllRooms() {
        return List.copyOf(rooms.values());
    }

    public Optional<Room> getRoomByNumber(String number) {
        return Optional.ofNullable(rooms.get(number));
    }
}
