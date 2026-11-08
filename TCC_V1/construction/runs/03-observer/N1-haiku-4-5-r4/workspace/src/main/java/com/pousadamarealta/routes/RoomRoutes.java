package com.pousadamarealta.routes;

import io.javalin.Javalin;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.ErrorResponse;

public class RoomRoutes {
    public static void register(Javalin app) throws Exception {
        RoomRepository roomRepository = new RoomRepository();

        app.get("/rooms", ctx -> {
            try {
                ctx.json(roomRepository.findAll());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });
    }
}
