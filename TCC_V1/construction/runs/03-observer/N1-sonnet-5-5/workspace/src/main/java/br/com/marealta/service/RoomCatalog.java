package br.com.marealta.service;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;

import java.util.List;

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

    public Room get(int number) {
        return rooms.stream().filter(room -> room.number() == number).findFirst()
                .orElseThrow(() -> new DomainException(ErrorCode.ROOM_NOT_FOUND, "Quarto não existe: " + number));
    }
}
