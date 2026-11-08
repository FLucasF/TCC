package com.marealata.handler;

import com.marealata.dao.RoomDao;
import com.marealata.model.Room;
import io.javalin.Javalin;
import java.sql.SQLException;
import java.util.List;

public class RoomHandler {
    public static void register(Javalin app) {
        app.get("/rooms", ctx -> {
            try {
                List<Room> rooms = RoomDao.findAll();
                ctx.json(rooms);
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
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
