package com.pousadamarealta.service;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import java.util.List;
import java.util.Map;

public class RoomService {
    private final Map<Integer, Room> rooms = Map.ofEntries(
        Map.entry(101, new Room(101, RoomType.STANDARD, 2, 249.90)),
        Map.entry(102, new Room(102, RoomType.STANDARD, 2, 249.90)),
        Map.entry(201, new Room(201, RoomType.DELUXE, 3, 389.90)),
        Map.entry(202, new Room(202, RoomType.DELUXE, 3, 389.90)),
        Map.entry(301, new Room(301, RoomType.SUITE, 4, 579.90))
    );

    public List<Room> getAllRooms() {
        return rooms.values().stream()
            .sorted((a, b) -> Integer.compare(a.number(), b.number()))
            .toList();
    }

    public Room getRoomByNumber(int number) {
        return rooms.get(number);
    }

    public boolean roomExists(int number) {
        return rooms.containsKey(number);
    }
}
