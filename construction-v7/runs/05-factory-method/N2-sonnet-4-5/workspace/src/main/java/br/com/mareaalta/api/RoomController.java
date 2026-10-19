package br.com.mareaalta.api;

import br.com.mareaalta.service.RoomService;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getRooms(Context ctx) {
        List<Map<String, Object>> rooms = roomService.getAllRooms().stream()
            .map(ApiMapper::toRoomResponse)
            .collect(Collectors.toList());
        ctx.json(rooms);
    }
}
