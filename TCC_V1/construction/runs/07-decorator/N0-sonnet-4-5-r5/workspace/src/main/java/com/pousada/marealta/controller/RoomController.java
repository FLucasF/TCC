package com.pousada.marealta.controller;

import com.pousada.marealta.dto.RoomResponse;
import com.pousada.marealta.service.RoomService;
import io.javalin.http.Context;

import java.util.List;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getRooms(Context ctx) {
        List<RoomResponse> rooms = roomService.getAllRooms().stream()
            .map(RoomResponse::from)
            .toList();
        ctx.json(rooms);
    }
}
