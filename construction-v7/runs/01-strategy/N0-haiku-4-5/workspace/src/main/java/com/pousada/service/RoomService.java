package com.pousada.service;

import com.pousada.domain.Room;
import com.pousada.exception.ApiException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RoomService {
    private final List<Room> rooms = new ArrayList<>();

    public RoomService() {
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.add(new Room(101, "STANDARD", 2, new BigDecimal("249.90")));
        rooms.add(new Room(102, "STANDARD", 2, new BigDecimal("249.90")));
        rooms.add(new Room(201, "DELUXE", 3, new BigDecimal("389.90")));
        rooms.add(new Room(202, "DELUXE", 3, new BigDecimal("389.90")));
        rooms.add(new Room(301, "SUITE", 4, new BigDecimal("579.90")));
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms);
    }

    public Room getRoomByNumber(int roomNumber) throws ApiException {
        return rooms.stream()
                .filter(r -> r.getNumber() == roomNumber)
                .findFirst()
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Room not found"));
    }
}
