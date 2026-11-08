package br.com.marealta.api.controller;

import br.com.marealta.api.dto.RoomResponse;
import br.com.marealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.List;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void listRooms(Context ctx) {
        List<RoomResponse> rooms = roomRepository.findAll().stream()
            .map(RoomResponse::from)
            .toList();

        ctx.json(rooms);
    }
}
