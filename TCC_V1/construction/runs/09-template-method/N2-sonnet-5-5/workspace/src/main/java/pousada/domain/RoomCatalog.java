package pousada.domain;

import java.util.List;
import java.util.Optional;

public class RoomCatalog {
    private final List<Room> rooms = List.of(
            Room.of(101, RoomType.STANDARD),
            Room.of(102, RoomType.STANDARD),
            Room.of(201, RoomType.DELUXE),
            Room.of(202, RoomType.DELUXE),
            Room.of(301, RoomType.SUITE));

    public List<Room> all() {
        return rooms;
    }

    public Optional<Room> find(int number) {
        return rooms.stream().filter(r -> r.number() == number).findFirst();
    }

    public Room get(int number) {
        return find(number).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + number + " não existe"));
    }
}
