package com.pousada.api;

import com.pousada.db.Room;
import com.pousada.service.RoomService;
import io.javalin.Javalin;
import java.sql.SQLException;

public class RoomAPI {
    public static void register(Javalin app) {
        app.get("/rooms", ctx -> {
            try {
                var rooms = RoomService.getAllRooms();
                ctx.json(rooms);
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
