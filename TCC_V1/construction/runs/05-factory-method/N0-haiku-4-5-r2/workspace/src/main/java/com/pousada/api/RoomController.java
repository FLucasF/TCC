package com.pousada.api;

import io.javalin.Javalin;
import com.google.gson.Gson;
import com.pousada.db.Database;
import com.pousada.model.Room;
import com.pousada.repository.RoomRepository;
import java.util.List;
import java.util.Map;

public class RoomController {
    private static final Gson gson = new Gson();

    public static void setup(Javalin app, Database db) {
        RoomRepository roomRepo = new RoomRepository(db);

        app.get("/rooms", ctx -> {
            List<Room> rooms = roomRepo.findAll();
            List<Map<String, Object>> response = rooms.stream().map(room -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("number", room.number);
                m.put("type", room.type);
                m.put("capacity", room.capacity);
                m.put("nightlyRate", room.nightlyRate);
                return m;
            }).toList();
            ctx.json(response);
        });
    }
}
