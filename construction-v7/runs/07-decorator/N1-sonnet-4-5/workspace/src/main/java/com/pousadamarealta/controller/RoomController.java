package com.pousadamarealta.controller;

import com.pousadamarealta.dto.response.RoomResponse;
import com.pousadamarealta.service.RoomService;
import io.javalin.http.Context;

import java.util.List;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getRooms(Context ctx) {
        List<RoomResponse> rooms = roomService.getAllRooms().stream()
            .map(RoomResponse::new)
            .toList();
        ctx.json(rooms);
    }
}
