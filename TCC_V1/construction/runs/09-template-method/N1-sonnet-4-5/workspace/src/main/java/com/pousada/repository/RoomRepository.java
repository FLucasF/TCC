package com.pousada.repository;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final List<Room> rooms;

    public RoomRepository() {
        this.rooms = new ArrayList<>();
        rooms.add(new Room("101", RoomType.STANDARD));
        rooms.add(new Room("102", RoomType.STANDARD));
        rooms.add(new Room("201", RoomType.DELUXE));
        rooms.add(new Room("202", RoomType.DELUXE));
        rooms.add(new Room("301", RoomType.SUITE));
    }

    public List<Room> findAll() {
        return new ArrayList<>(rooms);
    }

    public Optional<Room> findByNumber(String number) {
        return rooms.stream()
                .filter(room -> room.getNumber().equals(number))
                .findFirst();
    }

    public int getTotalRooms() {
        return rooms.size();
    }
}
