package com.pousada.service;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import java.util.List;
import java.util.Map;

public class RoomService {
    private final Map<Integer, Room> rooms;

    public RoomService() {
        this.rooms = Map.ofEntries(
                Map.entry(101, new Room(101, RoomType.STANDARD)),
                Map.entry(102, new Room(102, RoomType.STANDARD)),
                Map.entry(201, new Room(201, RoomType.DELUXE)),
                Map.entry(202, new Room(202, RoomType.DELUXE)),
                Map.entry(301, new Room(301, RoomType.SUITE)));
    }

    public List<Room> getAllRooms() {
        return rooms.values().stream().sorted((a, b) -> Integer.compare(a.number, b.number)).toList();
    }

    public Room getRoom(int number) {
        return rooms.get(number);
    }

    public boolean roomExists(int number) {
        return rooms.containsKey(number);
    }
}
