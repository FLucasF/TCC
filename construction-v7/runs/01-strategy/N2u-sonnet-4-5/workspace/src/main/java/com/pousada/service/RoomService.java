package com.pousada.service;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;

import java.util.List;
import java.util.Optional;

public class RoomService {
    private final List<Room> rooms;

    public RoomService() {
        this.rooms = List.of(
            new Room("101", RoomType.STANDARD),
            new Room("102", RoomType.STANDARD),
            new Room("201", RoomType.DELUXE),
            new Room("202", RoomType.DELUXE),
            new Room("301", RoomType.SUITE)
        );
    }

    public List<Room> getAllRooms() {
        return rooms;
    }

    public Optional<Room> findByNumber(String roomNumber) {
        return rooms.stream()
            .filter(room -> room.getNumber().equals(roomNumber))
            .findFirst();
    }
}
