package com.pousada.api;

import com.pousada.api.dto.RoomDto;
import com.pousada.infrastructure.RoomRepository;
import io.javalin.http.Context;

import java.util.List;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getRooms(Context ctx) {
        List<RoomDto> rooms = roomRepository.findAll().stream()
            .map(RoomDto::from)
            .toList();
        ctx.json(rooms);
    }
}
