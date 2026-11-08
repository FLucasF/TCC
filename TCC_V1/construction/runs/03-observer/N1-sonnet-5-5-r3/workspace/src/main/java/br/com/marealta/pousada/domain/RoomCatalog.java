package br.com.marealta.pousada.domain;

import java.util.List;
import java.util.Optional;

/** Os quartos da pousada; mudam tão raramente que ficam no código. */
public final class RoomCatalog {

    private static final List<Room> ROOMS = List.of(
            Room.of(101, RoomType.STANDARD),
            Room.of(102, RoomType.STANDARD),
            Room.of(201, RoomType.DELUXE),
            Room.of(202, RoomType.DELUXE),
            Room.of(301, RoomType.SUITE));

    private RoomCatalog() {
    }

    public static List<Room> all() {
        return ROOMS;
    }

    public static Optional<Room> find(int number) {
        return ROOMS.stream().filter(r -> r.number() == number).findFirst();
    }
}
