package com.pousada.marealta.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.infrastructure.persistence.RoomRepository;
import io.javalin.Javalin;

import java.util.List;
import java.util.Map;

public class RoomController {
    private final RoomRepository roomRepository;
    private final Gson gson = new GsonBuilder().create();

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void register(Javalin app) {
        app.get("/rooms", ctx -> {
            try {
                List<Room> rooms = roomRepository.findAll();
                List<Map<String, Object>> response = rooms.stream().map(room -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("number", room.getNumber());
                    m.put("type", room.getType().getName());
                    m.put("capacity", room.getCapacity());
                    m.put("nightlyRate", room.getNightlyRate());
                    return m;
                }).toList();
                ctx.json(response);
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
            }
        });
    }
}
