package com.pousada.service;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import com.pousada.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoomService {

    private final Map<String, Room> rooms;

    public RoomService() {
        this.rooms = Map.of(
            "101", new Room("101", RoomType.STANDARD),
            "102", new Room("102", RoomType.STANDARD),
            "201", new Room("201", RoomType.DELUXE),
            "202", new Room("202", RoomType.DELUXE),
            "301", new Room("301", RoomType.SUITE)
        );
    }

    public List<Room> getAllRooms() {
        return rooms.values().stream()
            .sorted((r1, r2) -> r1.getNumber().compareTo(r2.getNumber()))
            .collect(Collectors.toList());
    }

    public Room getRoom(String number) {
        Room room = rooms.get(number);
        if (room == null) {
            throw new BusinessException("ROOM_NOT_FOUND", "Room not found: " + number, 404);
        }
        return room;
    }
}
