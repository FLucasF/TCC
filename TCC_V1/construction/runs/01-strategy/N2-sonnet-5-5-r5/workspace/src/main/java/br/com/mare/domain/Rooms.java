package br.com.mare.domain;

import java.util.List;
import java.util.Optional;

public final class Rooms {
    private static final List<Room> ALL = List.of(
            new Room(101, RoomType.STANDARD, 2, Money.of("249.90")),
            new Room(102, RoomType.STANDARD, 2, Money.of("249.90")),
            new Room(201, RoomType.DELUXE, 3, Money.of("389.90")),
            new Room(202, RoomType.DELUXE, 3, Money.of("389.90")),
            new Room(301, RoomType.SUITE, 4, Money.of("579.90")));

    private Rooms() {}

    public static List<Room> all() { return ALL; }

    public static Optional<Room> find(int number) {
        return ALL.stream().filter(r -> r.number() == number).findFirst();
    }
}
