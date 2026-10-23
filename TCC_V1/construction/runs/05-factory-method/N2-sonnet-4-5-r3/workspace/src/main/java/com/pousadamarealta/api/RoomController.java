package com.pousadamarealta.api;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.service.RoomService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getRooms(Context ctx) throws SQLException {
        List<Room> rooms = roomService.getAllRooms();
        List<Map<String, Object>> response = rooms.stream()
            .map(this::mapRoomToResponse)
            .collect(Collectors.toList());
        ctx.json(response);
    }

    private Map<String, Object> mapRoomToResponse(Room room) {
        Map<String, Object> map = new HashMap<>();
        map.put("number", room.getNumber());
        map.put("type", room.getType().name());
        map.put("capacity", room.getCapacity());
        map.put("nightlyRate", room.getNightlyRate());
        return map;
    }
}
