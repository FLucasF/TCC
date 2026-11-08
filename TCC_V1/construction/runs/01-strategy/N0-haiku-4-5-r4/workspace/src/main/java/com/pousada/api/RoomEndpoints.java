package com.pousada.api;

import com.pousada.service.RoomService;
import io.javalin.Javalin;
import java.sql.SQLException;

public class RoomEndpoints {
    public static void register(Javalin app) {
        app.get("/rooms", ctx -> {
            try {
                var rooms = new RoomService().getAllRooms();
                var response = rooms.stream().map(r -> {
                    var obj = new java.util.LinkedHashMap<>();
                    obj.put("number", r.number);
                    obj.put("type", r.type);
                    obj.put("capacity", r.capacity);
                    obj.put("nightlyRate", r.nightlyRate.doubleValue());
                    return obj;
                }).toArray();
                ctx.json(response);
            } catch (SQLException e) {
                ctx.status(500).json(new Object() {
                    public final String error = "DATABASE_ERROR";
                    public final String message = e.getMessage();
                });
            }
        });
    }
}
