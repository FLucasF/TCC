package com.pousadamareaita.storage;

import com.pousadamareaita.model.Room;
import java.math.BigDecimal;
import java.util.*;

public class RoomRepository {
    private static final Map<Integer, Room> rooms = new LinkedHashMap<>();

    static {
        rooms.put(101, new Room(101, com.pousadamareaita.model.RoomType.STANDARD, 2, BigDecimal.valueOf(249.90)));
        rooms.put(102, new Room(102, com.pousadamareaita.model.RoomType.STANDARD, 2, BigDecimal.valueOf(249.90)));
        rooms.put(201, new Room(201, com.pousadamareaita.model.RoomType.DELUXE, 3, BigDecimal.valueOf(389.90)));
        rooms.put(202, new Room(202, com.pousadamareaita.model.RoomType.DELUXE, 3, BigDecimal.valueOf(389.90)));
        rooms.put(301, new Room(301, com.pousadamareaita.model.RoomType.SUITE, 4, BigDecimal.valueOf(579.90)));
    }

    public Room findByNumber(int number) {
        return rooms.get(number);
    }

    public Collection<Room> findAll() {
        return rooms.values();
    }
}
