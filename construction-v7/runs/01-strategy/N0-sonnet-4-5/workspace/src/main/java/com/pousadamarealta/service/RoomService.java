package com.pousadamarealta.service;

import com.pousadamarealta.exception.RoomNotFoundException;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomService {
    private final Map<String, Room> rooms = new HashMap<>();

    public RoomService() {
        rooms.put("101", new Room("101", RoomType.STANDARD));
        rooms.put("102", new Room("102", RoomType.STANDARD));
        rooms.put("201", new Room("201", RoomType.DELUXE));
        rooms.put("202", new Room("202", RoomType.DELUXE));
        rooms.put("301", new Room("301", RoomType.SUITE));
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Room getRoom(String roomNumber) {
        Room room = rooms.get(roomNumber);
        if (room == null) {
            throw new RoomNotFoundException();
        }
        return room;
    }
}
