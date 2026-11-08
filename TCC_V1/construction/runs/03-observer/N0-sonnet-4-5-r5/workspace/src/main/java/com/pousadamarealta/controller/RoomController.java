package com.pousadamarealta.controller;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.service.RoomService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getRooms(Context ctx) {
        List<Room> rooms = roomService.getAllRooms();
        List<Map<String, Object>> response = rooms.stream()
                .map(this::roomToMap)
                .toList();
        ctx.json(response);
    }

    private Map<String, Object> roomToMap(Room room) {
        Map<String, Object> map = new HashMap<>();
        map.put("number", room.getNumber());
        map.put("type", room.getType().name());
        map.put("capacity", room.getCapacity());
        map.put("nightlyRate", room.getNightlyRate());
        return map;
    }
}
