package com.pousadamarealta.controller;

import com.pousadamarealta.api.RoomResponse;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.service.RoomService;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.List;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getRooms(Context ctx) {
        List<RoomResponse> rooms = new ArrayList<>();
        for (Room room : roomService.getAllRooms()) {
            rooms.add(new RoomResponse(
                room.getNumber(),
                room.getType().getCode(),
                room.getCapacity(),
                room.getNightlyRate()
            ));
        }
        ctx.json(rooms);
    }
}
