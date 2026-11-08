package com.marealta.domain;

import java.util.List;
import java.util.Optional;

public final class RoomCatalog {
    private final List<Room> rooms = List.of(
            new Room(101, RoomType.STANDARD, 2, Money.of("249.90")),
            new Room(102, RoomType.STANDARD, 2, Money.of("249.90")),
            new Room(201, RoomType.DELUXE, 3, Money.of("389.90")),
            new Room(202, RoomType.DELUXE, 3, Money.of("389.90")),
            new Room(301, RoomType.SUITE, 4, Money.of("579.90")));

    public List<Room> all() {
        return rooms;
    }

    public Optional<Room> find(int number) {
        return rooms.stream().filter(r -> r.number() == number).findFirst();
    }

    public Room require(int number) {
        return find(number).orElseThrow(() ->
                new PousadaException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + number + " não existe"));
    }

    public int size() {
        return rooms.size();
    }
}
