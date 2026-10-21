package com.pousada.controller;

import com.pousada.domain.Room;
import com.pousada.service.RoomService;
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
            .map(this::toRoomDto)
            .collect(Collectors.toList());
        ctx.json(response);
    }

    private Map<String, Object> toRoomDto(Room room) {
        return Map.of(
            "number", room.getNumber(),
            "type", room.getType().toString(),
            "capacity", room.getCapacity(),
            "nightlyRate", room.getNightlyRate()
        );
    }
}
