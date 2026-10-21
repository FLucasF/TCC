package com.pousada.controller;

import com.pousada.dto.RoomResponse;
import com.pousada.service.RoomService;
import io.javalin.Javalin;

import java.util.List;
import java.util.stream.Collectors;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/rooms", ctx -> {
            List<RoomResponse> rooms = roomService.getAllRooms().stream()
                .map(RoomResponse::new)
                .collect(Collectors.toList());
            ctx.json(rooms);
        });
    }
}
