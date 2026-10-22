package com.pousadamarealta.api;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();

        List<Map<String, Object>> response = rooms.stream()
                .map(room -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("number", room.getNumber());
                    map.put("type", room.getType().name());
                    map.put("capacity", room.getCapacity());
                    map.put("nightlyRate", room.getNightlyRate());
                    return map;
                })
                .toList();

        ctx.json(response);
    }
}
