package com.pousada.api;

import com.pousada.api.dto.RoomResponse;
import com.pousada.service.RoomService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.List;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void register(Javalin app) {
        app.get("/rooms", this::getRooms);
    }

    private void getRooms(Context ctx) {
        List<RoomResponse> rooms = roomService.getAllRooms().stream()
            .map(RoomResponse::new)
            .toList();
        ctx.status(HttpStatus.OK);
        ctx.json(rooms);
    }
}
