package com.pousadamarealta.controller;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController() {
        this.roomRepository = new RoomRepository();
    }

    public void listRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> response = rooms.stream()
            .map(this::toJson)
            .collect(Collectors.toList());
        ctx.json(response);
    }

    private Map<String, Object> toJson(Room room) {
        Map<String, Object> json = new HashMap<>();
        json.put("number", room.getNumber());
        json.put("type", room.getType().name());
        json.put("capacity", room.getCapacity());
        json.put("nightlyRate", room.getNightlyRate());
        return json;
    }
}
