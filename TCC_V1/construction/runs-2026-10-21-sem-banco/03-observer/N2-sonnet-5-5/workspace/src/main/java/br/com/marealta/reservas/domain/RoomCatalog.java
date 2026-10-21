package br.com.marealta.reservas.domain;

import java.util.List;
import java.util.Optional;

public class RoomCatalog {
    private final List<Room> rooms;

    public RoomCatalog(List<Room> rooms) {
        this.rooms = List.copyOf(rooms);
    }

    public static RoomCatalog marealta() {
        return new RoomCatalog(List.of(
                new Room(101, RoomType.STANDARD),
                new Room(102, RoomType.STANDARD),
                new Room(201, RoomType.DELUXE),
                new Room(202, RoomType.DELUXE),
                new Room(301, RoomType.SUITE)));
    }

    public List<Room> all() { return rooms; }

    public Optional<Room> find(int number) {
        return rooms.stream().filter(r -> r.number() == number).findFirst();
    }
}
