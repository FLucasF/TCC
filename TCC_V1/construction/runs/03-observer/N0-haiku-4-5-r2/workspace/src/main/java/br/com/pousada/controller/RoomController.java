package br.com.pousada.controller;

import br.com.pousada.model.Room;
import br.com.pousada.repository.RoomRepository;
import io.javalin.Javalin;

public class RoomController {
    private static final RoomRepository roomRepository = new RoomRepository();

    public static void routes(Javalin app) {
        app.get("/rooms", ctx -> {
            var rooms = roomRepository.findAll();
            ctx.json(rooms);
        });
    }
}
