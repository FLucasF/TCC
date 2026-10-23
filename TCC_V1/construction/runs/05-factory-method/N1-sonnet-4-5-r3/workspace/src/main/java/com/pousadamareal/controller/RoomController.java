package com.pousadamareal.controller;

import com.pousadamareal.api.RoomResponse;
import com.pousadamareal.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.List;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void listRooms(Context ctx) {
        List<RoomResponse> rooms = roomRepository.findAll().stream()
            .map(RoomResponse::new)
            .toList();
        ctx.json(rooms);
    }
}
