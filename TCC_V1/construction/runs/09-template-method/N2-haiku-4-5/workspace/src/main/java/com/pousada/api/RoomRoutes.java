package com.pousada.api;

import com.pousada.domain.Room;
import com.pousada.repo.RoomRepository;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class RoomRoutes {
    public static void register(Javalin app, RoomRepository roomRepo) {
        app.get("/rooms", ctx -> {
            try {
                List<Room> rooms = roomRepo.findAll();
                JSONArray array = new JSONArray();
                for (Room room : rooms) {
                    array.put(new JSONObject()
                        .put("number", room.getNumber())
                        .put("type", room.getType().getDisplayName())
                        .put("capacity", room.getCapacity())
                        .put("nightlyRate", room.getNightlyRate().doubleValue())
                    );
                }
                ctx.json(array.toString());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new JSONObject()
                    .put("error", "INTERNAL_ERROR")
                    .put("message", e.getMessage())
                    .toString());
            }
        });
    }
}
