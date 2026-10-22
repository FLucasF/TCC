package br.com.maralta.pousada;

import java.util.List;
import java.util.Optional;

/** Os quartos da pousada. */
public final class Rooms {
    public static final List<Room> ALL = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));

    private Rooms() {
    }

    public static Optional<Room> find(int number) {
        return ALL.stream().filter(r -> r.number() == number).findFirst();
    }

    public static Room get(int number) {
        return find(number).orElseThrow(() -> ApiException.roomNotFound(number));
    }
}
