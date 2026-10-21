package br.com.pousadamarealta.reservas.domain;

import java.util.List;
import java.util.Optional;

public final class RoomCatalog {

    private final List<Room> rooms;

    public RoomCatalog(List<Room> rooms) {
        this.rooms = List.copyOf(rooms);
    }

    public static RoomCatalog marealta() {
        return new RoomCatalog(List.of(
                Room.of(101, RoomType.STANDARD),
                Room.of(102, RoomType.STANDARD),
                Room.of(201, RoomType.DELUXE),
                Room.of(202, RoomType.DELUXE),
                Room.of(301, RoomType.SUITE)));
    }

    public List<Room> all() {
        return rooms;
    }

    public Optional<Room> find(int number) {
        return rooms.stream().filter(room -> room.number() == number).findFirst();
    }

    public int size() {
        return rooms.size();
    }
}
