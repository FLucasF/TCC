package com.pousadamarealta.api.controller;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.service.RoomService;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getAllRooms(Context ctx) {
        List<Room> rooms = roomService.getAllRooms();
        List<Map<String, Object>> response = rooms.stream()
            .map(room -> {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("number", room.getNumber());
                map.put("type", room.getType().name());
                map.put("capacity", room.getCapacity());
                map.put("nightlyRate", room.getNightlyRate());
                return map;
            })
            .collect(Collectors.toList());

        ctx.json(response);
    }
}
