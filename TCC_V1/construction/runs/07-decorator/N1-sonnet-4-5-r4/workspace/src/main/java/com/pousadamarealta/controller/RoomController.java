package com.pousadamarealta.controller;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getAll(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> response = rooms.stream()
                .map(this::toMap)
                .toList();
        ctx.json(response);
    }

    private Map<String, Object> toMap(Room room) {
        Map<String, Object> map = new HashMap<>();
        map.put("number", room.getNumber());
        map.put("type", room.getType().name());
        map.put("capacity", room.getCapacity());
        map.put("nightlyRate", room.getNightlyRate());
        return map;
    }
}
