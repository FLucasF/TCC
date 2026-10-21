package com.pousamarealta.controllers;

import io.javalin.Javalin;
import com.pousamarealta.services.RoomService;

public class RoomController {
    private RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void setupRoutes(Javalin app) {
        app.get("/rooms", ctx -> {
            ctx.json(roomService.getAllRooms());
        });
    }
}
