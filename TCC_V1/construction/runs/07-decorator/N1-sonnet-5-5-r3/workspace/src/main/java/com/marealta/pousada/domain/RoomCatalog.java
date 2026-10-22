package com.marealta.pousada.domain;

import java.util.List;
import java.util.Optional;

public final class RoomCatalog {
    private static final List<Room> ROOMS = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));

    public List<Room> all() {
        return ROOMS;
    }

    public Optional<Room> find(int number) {
        return ROOMS.stream().filter(room -> room.number() == number).findFirst();
    }
}
