package com.pousada.domain;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Os quartos da pousada. Quase nunca mudam, então ficam aqui e não no banco. */
public class RoomCatalog {
    private final List<Room> rooms = List.of(
            new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90")));

    public List<Room> all() {
        return rooms.stream().sorted(Comparator.comparingInt(Room::number)).toList();
    }

    public Optional<Room> find(int number) {
        return rooms.stream().filter(r -> r.number() == number).findFirst();
    }

    public Room get(int number) {
        return find(number).orElseThrow(
                () -> new DomainException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + number + " não existe"));
    }

    public int size() {
        return rooms.size();
    }
}
