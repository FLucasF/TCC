package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.RoomResponse;
import com.pousadamarealta.domain.service.RoomService;
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
