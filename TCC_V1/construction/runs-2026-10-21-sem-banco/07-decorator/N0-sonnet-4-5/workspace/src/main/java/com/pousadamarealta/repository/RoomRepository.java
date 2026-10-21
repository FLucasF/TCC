package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;
import java.util.*;

public class RoomRepository {
    private final Map<String, Room> rooms = new HashMap<>();

    public RoomRepository() {
        rooms.put("101", new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.put("102", new Room("102", RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.put("201", new Room("201", RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.put("202", new Room("202", RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.put("301", new Room("301", RoomType.SUITE, 4, new BigDecimal("579.90")));
    }

    public Optional<Room> findByNumber(String number) {
        return Optional.ofNullable(rooms.get(number));
    }

    public List<Room> findAll() {
        return new ArrayList<>(rooms.values());
    }
}
