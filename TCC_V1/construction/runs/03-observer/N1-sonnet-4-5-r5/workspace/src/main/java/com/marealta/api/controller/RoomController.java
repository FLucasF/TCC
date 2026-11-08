package com.marealta.api.controller;

import com.marealta.api.dto.RoomResponse;
import com.marealta.domain.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.List;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void list(Context ctx) {
        List<RoomResponse> rooms = roomRepository.findAll().stream()
            .map(RoomResponse::from)
            .toList();
        ctx.json(rooms);
    }
}
