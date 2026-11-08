package com.pousada.repository;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private static final List<Room> ROOMS = List.of(
        new Room("101", RoomType.STANDARD),
        new Room("102", RoomType.STANDARD),
        new Room("201", RoomType.DELUXE),
        new Room("202", RoomType.DELUXE),
        new Room("301", RoomType.SUITE)
    );

    public List<Room> findAll() {
        return new ArrayList<>(ROOMS);
    }

    public Optional<Room> findByNumber(String number) {
        return ROOMS.stream()
            .filter(room -> room.getNumber().equals(number))
            .findFirst();
    }
}
