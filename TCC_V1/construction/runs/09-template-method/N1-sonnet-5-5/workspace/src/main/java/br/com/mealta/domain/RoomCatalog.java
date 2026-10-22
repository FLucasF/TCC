package br.com.mealta.domain;

import java.util.List;
import java.util.Optional;

public class RoomCatalog {
    private final List<Room> rooms = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));

    public List<Room> all() {
        return rooms;
    }

    public Optional<Room> find(int number) {
        return rooms.stream().filter(r -> r.number() == number).findFirst();
    }

    public Room require(int number) {
        return find(number).orElseThrow(() ->
                new PousadaException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + number + " não existe"));
    }
}
