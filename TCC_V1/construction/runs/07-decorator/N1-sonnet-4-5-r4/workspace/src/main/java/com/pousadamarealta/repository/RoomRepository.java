package com.pousadamarealta.repository;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private static final List<Room> ROOMS = Arrays.asList(
        new Room("101", RoomType.STANDARD),
        new Room("102", RoomType.STANDARD),
        new Room("201", RoomType.DELUXE),
        new Room("202", RoomType.DELUXE),
        new Room("301", RoomType.SUITE)
    );

    public List<Room> findAll() {
        return ROOMS;
    }

    public Optional<Room> findByNumber(String number) {
        return ROOMS.stream()
                .filter(room -> room.getNumber().equals(number))
                .findFirst();
    }
}
