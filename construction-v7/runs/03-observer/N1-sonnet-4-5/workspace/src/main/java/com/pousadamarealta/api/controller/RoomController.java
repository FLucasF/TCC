package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.dto.RoomDTO;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.service.RoomService;
import io.javalin.http.Context;

import java.util.List;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getRooms(Context ctx) {
        List<RoomDTO> rooms = roomService.getAllRooms().stream()
            .map(this::toDTO)
            .toList();
        ctx.json(rooms);
    }

    private RoomDTO toDTO(Room room) {
        return new RoomDTO(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getNightlyRate().doubleValue()
        );
    }
}
