package com.pousadamarealta.service;

import com.pousadamarealta.domain.Room;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RoomService {
    private final List<Room> rooms;

    public RoomService() {
        this.rooms = new ArrayList<>();
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
        return Collections.unmodifiableList(rooms);
    }

    public Optional<Room> getRoomByNumber(int number) {
        return rooms.stream()
                .filter(r -> r.getNumber() == number)
                .findFirst();
    }
}
