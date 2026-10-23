package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.RoomResponse;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.List;

public final class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getRooms(Context ctx) {
        List<RoomResponse> rooms = roomRepository.findAll().stream()
            .map(RoomResponse::from)
            .toList();
        ctx.json(rooms);
    }
}
