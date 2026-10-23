package br.com.pousada.maralta.controller;

import br.com.pousada.maralta.domain.Room;
import br.com.pousada.maralta.service.RoomService;
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
            .map(room -> {
                Map<String, Object> map = new HashMap<>();
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
