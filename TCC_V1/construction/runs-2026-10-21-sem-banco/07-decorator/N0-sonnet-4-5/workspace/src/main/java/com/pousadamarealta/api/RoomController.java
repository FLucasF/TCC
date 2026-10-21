package com.pousadamarealta.api;

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

    public void getRooms(Context ctx) {
        List<Map<String, Object>> rooms = roomRepository.findAll().stream()
                .map(room -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("number", room.getNumber());
                    map.put("type", room.getType());
                    map.put("capacity", room.getCapacity());
                    map.put("nightlyRate", room.getNightlyRate());
                    return map;
                })
                .toList();

        ctx.json(rooms);
    }
}
