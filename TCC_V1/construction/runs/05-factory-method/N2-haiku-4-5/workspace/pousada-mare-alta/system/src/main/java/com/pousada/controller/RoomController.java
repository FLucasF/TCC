package com.pousada.controller;

import com.pousada.dto.RoomResponse;
import com.pousada.service.RoomService;
import io.javalin.Javalin;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/rooms", ctx -> {
            var rooms = roomService.getAllRooms();
            var responses = rooms.stream().map(RoomResponse::fromRoom).toList();
            ctx.json(responses);
        });
    }
}
