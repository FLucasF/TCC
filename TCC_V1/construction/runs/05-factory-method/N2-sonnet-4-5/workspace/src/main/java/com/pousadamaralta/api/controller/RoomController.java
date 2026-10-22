package com.pousadamaralta.api.controller;

import com.pousadamaralta.api.dto.RoomResponse;
import com.pousadamaralta.domain.service.RoomService;
import io.javalin.http.Context;
import java.util.List;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void list(Context ctx) {
        List<RoomResponse> rooms = roomService.getAllRooms().stream()
            .map(RoomResponse::from)
            .toList();
        ctx.json(rooms);
    }
}
