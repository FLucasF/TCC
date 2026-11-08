package com.pousada;

import com.google.gson.Gson;
import com.pousada.api.dto.RoomResponse;
import com.pousada.infrastructure.RoomRepository;
import io.javalin.http.Context;

import java.util.List;

public class RoomsHandler {
    private final RoomRepository roomRepository = new RoomRepository();
    private final Gson gson = new Gson();

    public void list(Context ctx) {
        try {
            var rooms = roomRepository.findAll();
            var response = rooms.stream().map(RoomResponse::fromDomain).toList();
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    static class ErrorResponse {
        String error;
        String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
