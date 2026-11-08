package com.pousadamarealta.infrastructure.web;

import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.service.RoomService;
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
            .map(this::toRoomDto)
            .toList();
        ctx.json(response);
    }

    private Map<String, Object> toRoomDto(Room room) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("number", room.getNumber());
        dto.put("type", room.getType().name());
        dto.put("capacity", room.getCapacity());
        dto.put("nightlyRate", room.getType().getNightlyRate());
        return dto;
    }
}
