package hotel.rooms;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** The inn's rooms. They rarely change, so they live in the code. */
public final class Rooms {

    private final List<Room> allRooms = List.of(
            new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90")));

    public List<Room> all() {
        return allRooms;
    }

    public Optional<Room> find(int roomNumber) {
        return allRooms.stream().filter(room -> room.number() == roomNumber).findFirst();
    }
}
