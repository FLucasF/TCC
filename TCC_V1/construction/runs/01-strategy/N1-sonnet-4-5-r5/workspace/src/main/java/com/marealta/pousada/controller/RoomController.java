package com.marealta.pousada.controller;

import com.marealta.pousada.dto.RoomDto;
import com.marealta.pousada.service.RoomService;
import io.javalin.http.Context;

import java.util.List;

public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getAllRooms(Context ctx) {
        List<RoomDto> rooms = roomService.getAllRooms().stream()
            .map(RoomDto::from)
            .toList();
        ctx.json(rooms);
    }
}
