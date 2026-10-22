package com.pousada.marealta.handlers;

import com.pousada.marealta.repository.RoomRepository;
import io.javalin.Javalin;

public class RoomHandler {

    public static void register(Javalin app) {
        app.get("/rooms", ctx -> {
            try {
                var rooms = RoomRepository.findAll();
                ctx.json(rooms);
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });
    }

    record ErrorResponse(String error, String message) {}
}
