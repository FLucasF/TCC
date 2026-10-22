package pousada.domain;

import java.util.List;
import java.util.Optional;

/** Os quartos da pousada. */
public final class Rooms {
    private static final List<Room> ALL = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));

    private Rooms() {
    }

    public static List<Room> all() {
        return ALL;
    }

    public static Optional<Room> find(int number) {
        return ALL.stream().filter(r -> r.number() == number).findFirst();
    }
}
