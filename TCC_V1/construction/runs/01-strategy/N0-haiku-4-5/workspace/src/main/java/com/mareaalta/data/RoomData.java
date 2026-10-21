package com.mareaalta.data;

import com.mareaalta.model.Room;
import java.util.List;

public class RoomData {
    public static List<Room> getRooms() {
        return List.of(
            new Room(101, "STANDARD", 2, 249.90),
            new Room(102, "STANDARD", 2, 249.90),
            new Room(201, "DELUXE", 3, 389.90),
            new Room(202, "DELUXE", 3, 389.90),
            new Room(301, "SUITE", 4, 579.90)
        );
    }
}
