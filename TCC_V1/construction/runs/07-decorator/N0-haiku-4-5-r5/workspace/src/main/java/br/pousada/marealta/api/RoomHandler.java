package br.pousada.marealta.api;

import io.javalin.Javalin;
import br.pousada.marealta.database.RoomRepository;

public class RoomHandler {
    public static void register(Javalin app) {
        app.get("/rooms", ctx -> {
            try {
                var rooms = RoomRepository.findAll();
                ctx.json(rooms);
            } catch (Exception e) {
                ctx.status(500).json(ErrorResponse.error("INTERNAL_ERROR", "Failed to fetch rooms"));
            }
        });
    }
}
