package com.pousadamarealta.web.controller;

import com.pousadamarealta.service.RoomService;
import com.pousadamarealta.web.dto.RoomDTO;
import com.pousadamarealta.web.mapper.DomainMapper;
import io.javalin.http.Context;

import java.util.List;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getAllRooms(Context ctx) {
        List<RoomDTO> rooms = roomService.getAllRooms().stream()
            .map(DomainMapper::toDTO)
            .toList();
        ctx.json(rooms);
    }
}
