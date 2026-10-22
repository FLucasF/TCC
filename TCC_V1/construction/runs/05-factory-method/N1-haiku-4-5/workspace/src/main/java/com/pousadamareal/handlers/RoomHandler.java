package com.pousadamareal.handlers;

import com.pousadamareal.dao.RoomDAO;
import com.pousadamareal.model.Room;
import io.javalin.http.Context;

import java.util.List;

public class RoomHandler {
    public static void getRooms(Context ctx) {
        try {
            List<Room> rooms = RoomDAO.getAllRooms();
            ctx.json(rooms);
        } catch (Exception e) {
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            ctx.status(500);
        }
    }

    public record ErrorResponse(String error, String message) {}
}
