package com.pousadamarealta.service;

import com.pousadamarealta.model.ApiException;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.math.BigDecimal;
import java.util.*;

public class RoomService {
    private final Map<String, Room> rooms = new LinkedHashMap<>();

    public RoomService() {
        rooms.put("101", new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.put("102", new Room("102", RoomType.STANDARD, 2, new BigDecimal("249.90")));
        rooms.put("201", new Room("201", RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.put("202", new Room("202", RoomType.DELUXE, 3, new BigDecimal("389.90")));
        rooms.put("301", new Room("301", RoomType.SUITE, 4, new BigDecimal("579.90")));
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Room getRoom(String roomNumber) {
        Room room = rooms.get(roomNumber);
        if (room == null) {
            throw ApiException.roomNotFound();
        }
        return room;
    }
}
