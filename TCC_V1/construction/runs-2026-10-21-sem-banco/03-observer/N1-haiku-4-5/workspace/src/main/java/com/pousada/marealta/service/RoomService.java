package com.pousada.marealta.service;

import java.util.List;
import java.util.Optional;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.model.RoomType;

public class RoomService {
    private final List<Room> rooms;

    public RoomService() {
        this.rooms = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE)
        );
    }

    public List<Room> getAllRooms() {
        return rooms;
    }

    public Optional<Room> getRoomByNumber(int number) {
        return rooms.stream()
            .filter(r -> r.number == number)
            .findFirst();
    }
}
