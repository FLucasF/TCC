package com.pousadamarealta.handler;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.service.RoomService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomHandler {
    private final RoomService roomService;

    public RoomHandler(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getRooms(Context ctx) {
        List<Map<String, Object>> rooms = roomService.getAllRooms().stream()
            .map(this::toDto)
            .toList();
        ctx.json(rooms);
    }

    private Map<String, Object> toDto(Room room) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("number", room.getNumber());
        dto.put("type", room.getType().toString());
        dto.put("capacity", room.getCapacity());
        dto.put("nightlyRate", room.getNightlyRate());
        return dto;
    }
}
