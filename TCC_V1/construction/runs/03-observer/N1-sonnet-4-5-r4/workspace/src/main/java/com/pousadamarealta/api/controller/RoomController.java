package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.dto.RoomResponse;
import com.pousadamarealta.domain.service.RoomService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getRooms(Context ctx) throws SQLException {
        List<RoomResponse> rooms = roomService.getAllRooms().stream()
            .map(RoomResponse::new)
            .collect(Collectors.toList());
        ctx.json(rooms);
    }
}
