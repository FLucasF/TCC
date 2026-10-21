package com.pousada.repository;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RoomRepository {
    private final Map<Integer, Room> rooms = new HashMap<>();

    public RoomRepository() {
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.put(101, new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.put(102, new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.put(201, new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.put(202, new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.put(301, new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90")));
    }

    public Optional<Room> findById(int roomNumber) {
        return Optional.ofNullable(rooms.get(roomNumber));
    }

    public Map<Integer, Room> findAll() {
        return new HashMap<>(rooms);
    }
}
