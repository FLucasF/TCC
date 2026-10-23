package br.com.pousada.marealta.controller;

import br.com.pousada.marealta.model.Room;
import br.com.pousada.marealta.repository.RoomRepository;
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
        List<Room> rooms = roomRepository.findAll();
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
        dto.put("nightlyRate", room.getNightlyRate());
        return dto;
    }
}
