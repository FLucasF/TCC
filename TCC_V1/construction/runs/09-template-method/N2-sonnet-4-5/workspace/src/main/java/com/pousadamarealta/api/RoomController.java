package com.pousadamarealta.api;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void listRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> response = rooms.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        ctx.json(response);
    }

    private Map<String, Object> toDto(Room room) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("number", room.getNumber());
        dto.put("type", room.getType().name());
        dto.put("capacity", room.getCapacity());
        dto.put("nightlyRate", room.getNightlyRate());
        return dto;
    }
}
