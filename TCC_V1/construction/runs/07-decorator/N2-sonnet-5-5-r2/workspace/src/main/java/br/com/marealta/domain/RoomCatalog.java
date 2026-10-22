package br.com.marealta.domain;

import java.util.List;
import java.util.Optional;

public final class RoomCatalog {
    private static final List<Room> ROOMS = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));

    private RoomCatalog() {}

    public static List<Room> all() { return ROOMS; }

    public static Optional<Room> find(int number) {
        return ROOMS.stream().filter(r -> r.number() == number).findFirst();
    }
}
