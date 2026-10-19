package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final List<Room> rooms;

    public RoomRepository() {
        this.rooms = new ArrayList<>();
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.add(new Room(101, RoomType.STANDARD));
        rooms.add(new Room(102, RoomType.STANDARD));
        rooms.add(new Room(201, RoomType.DELUXE));
        rooms.add(new Room(202, RoomType.DELUXE));
        rooms.add(new Room(301, RoomType.SUITE));
    }

    public List<Room> findAll() {
        return new ArrayList<>(rooms);
    }

    public Optional<Room> findByNumber(int number) {
        return rooms.stream()
                .filter(r -> r.getNumber() == number)
                .findFirst();
    }
}
