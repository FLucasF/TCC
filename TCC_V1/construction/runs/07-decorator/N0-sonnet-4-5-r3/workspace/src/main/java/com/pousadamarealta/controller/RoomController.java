package com.pousadamarealta.controller;

import com.pousadamarealta.dto.RoomDTO;
import com.pousadamarealta.mapper.ModelMapper;
import com.pousadamarealta.service.RoomService;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getRooms(Context ctx) {
        List<RoomDTO> rooms = roomService.getAllRooms().stream()
            .map(ModelMapper::toDTO)
            .collect(Collectors.toList());
        ctx.json(rooms);
    }
}
