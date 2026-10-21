package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RoomRepository {
    private final Map<Integer, Room> rooms = Map.of(
        101, new Room(101, RoomType.STANDARD),
        102, new Room(102, RoomType.STANDARD),
        201, new Room(201, RoomType.DELUXE),
        202, new Room(202, RoomType.DELUXE),
        301, new Room(301, RoomType.SUITE)
    );

    public List<Room> findAll() {
        return rooms.values().stream()
            .sorted((r1, r2) -> Integer.compare(r1.number(), r2.number()))
            .toList();
    }

    public Optional<Room> findByNumber(int number) {
        return Optional.ofNullable(rooms.get(number));
    }
}
