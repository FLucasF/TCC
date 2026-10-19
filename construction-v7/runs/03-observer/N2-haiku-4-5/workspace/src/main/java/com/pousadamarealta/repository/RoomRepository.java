package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private static final List<Room> ROOMS = List.of(
        new Room(101, RoomType.STANDARD),
        new Room(102, RoomType.STANDARD),
        new Room(201, RoomType.DELUXE),
        new Room(202, RoomType.DELUXE),
        new Room(301, RoomType.SUITE)
    );

    public Optional<Room> findByNumber(int number) {
        return ROOMS.stream().filter(r -> r.number() == number).findFirst();
    }

    public List<Room> findAll() {
        return ROOMS;
    }
}
