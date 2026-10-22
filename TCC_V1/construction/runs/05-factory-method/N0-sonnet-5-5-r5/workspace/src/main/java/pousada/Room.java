package pousada;

import java.util.List;
import java.util.Optional;

public record Room(int number, RoomType type) {
    public static final List<Room> ALL = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));

    public static Optional<Room> find(int number) {
        return ALL.stream().filter(r -> r.number == number).findFirst();
    }

    public int capacity() {
        return type.capacity;
    }
}
