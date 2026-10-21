package com.pousadamarealta.api;

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

    public void getAllRooms(Context ctx) {
        List<Room> rooms = roomService.getAllRooms();
        List<Map<String, Object>> response = rooms.stream()
            .map(this::toDto)
            .toList();
        ctx.json(response);
    }

    private Map<String, Object> toDto(Room room) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("number", room.number());
        dto.put("type", room.type().name());
        dto.put("capacity", room.capacity());
        dto.put("nightlyRate", room.nightlyRate().doubleValue());
        return dto;
    }
}
