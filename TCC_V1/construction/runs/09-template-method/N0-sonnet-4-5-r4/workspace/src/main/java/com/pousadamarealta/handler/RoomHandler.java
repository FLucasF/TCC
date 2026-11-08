package com.pousadamarealta.handler;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomHandler {
    private final RoomRepository roomRepository;

    public RoomHandler(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getAllRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> response = rooms.stream()
            .map(room -> {
                Map<String, Object> map = new HashMap<>();
                map.put("number", room.getNumber());
                map.put("type", room.getType().name());
                map.put("capacity", room.getCapacity());
                map.put("nightlyRate", room.getNightlyRate().doubleValue());
                return map;
            })
            .toList();
        ctx.json(response);
    }
}
