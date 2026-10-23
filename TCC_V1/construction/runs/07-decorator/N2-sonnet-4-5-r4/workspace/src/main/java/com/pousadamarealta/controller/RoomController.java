package com.pousadamarealta.controller;

import com.pousadamarealta.dto.RoomDTO;
import com.pousadamarealta.service.RoomService;
import io.javalin.http.Context;

import java.util.List;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getAllRooms(Context ctx) {
        List<RoomDTO> rooms = roomService.getAllRooms().stream()
            .map(RoomDTO::from)
            .toList();
        ctx.json(rooms);
    }
}
