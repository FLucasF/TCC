package br.com.marealta.service;

import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;

import java.util.List;
import java.util.Optional;

public class RoomCatalog {
    private final List<Room> rooms = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));

    public List<Room> all() { return rooms; }

    public Optional<Room> find(int number) {
        return rooms.stream().filter(room -> room.number() == number).findFirst();
    }
}
