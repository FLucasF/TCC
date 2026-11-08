package br.com.maralta.controller;

import br.com.maralta.controller.dto.RoomResponse;
import br.com.maralta.repository.RoomRepository;
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
