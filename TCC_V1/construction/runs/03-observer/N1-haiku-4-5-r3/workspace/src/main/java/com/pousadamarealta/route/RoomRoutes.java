package com.pousadamarealta.route;

import com.pousadamarealta.dto.ErrorResponse;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.Javalin;

import java.util.List;

public class RoomRoutes {
    public static void register(Javalin app, RoomRepository roomRepository) {
        app.get("/rooms", ctx -> {
            try {
                List<Room> rooms = roomRepository.findAll();
                ctx.json(rooms);
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });
    }
}
