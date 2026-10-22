package br.com.marealte.controller;

import br.com.marealte.repository.RoomRepository;
import io.javalin.http.Context;

public class RoomController {

    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getRooms(Context ctx) {
        ctx.json(roomRepository.findAll());
    }
}
