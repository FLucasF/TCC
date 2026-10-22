package br.com.marealta.domain;

import java.util.List;
import java.util.Optional;

public record Room(int number, RoomType type) {

    private static final List<Room> ALL = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));

    public static List<Room> all() {
        return ALL;
    }

    public static Optional<Room> find(int number) {
        return ALL.stream().filter(room -> room.number == number).findFirst();
    }

    public int capacity() {
        return type.capacity();
    }
}
