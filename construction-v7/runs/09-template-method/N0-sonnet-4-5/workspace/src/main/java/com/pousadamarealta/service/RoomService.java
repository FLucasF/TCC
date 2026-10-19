package com.pousadamarealta.service;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomService {
    private final List<Room> rooms;

    public RoomService() {
        this.rooms = new ArrayList<>();
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.add(new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.add(new Room("102", RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.add(new Room("201", RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.add(new Room("202", RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.add(new Room("301", RoomType.SUITE, 4, new BigDecimal("579.90")));
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms);
    }

    public Optional<Room> findByNumber(String number) {
        return rooms.stream()
                .filter(room -> room.getNumber().equals(number))
                .findFirst();
    }
}
