package com.pousadamareaalta.service;

import com.pousadamareaalta.model.Room;
import com.pousadamareaalta.model.RoomType;
import java.math.BigDecimal;
import java.util.*;

public class RoomService {
    private final Map<Integer, Room> rooms;

    public RoomService() {
        this.rooms = new LinkedHashMap<>();
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.put(101, new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.put(102, new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.put(201, new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.put(202, new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.put(301, new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90")));
    }

    public Room getRoom(int number) {
        return rooms.get(number);
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }
}
