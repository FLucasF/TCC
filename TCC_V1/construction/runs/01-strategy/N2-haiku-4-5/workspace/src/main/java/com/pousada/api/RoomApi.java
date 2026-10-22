package com.pousada.api;

import com.pousada.domain.Room;
import com.pousada.repository.RoomRepository;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;

public class RoomApi {
    private static final RoomRepository roomRepo = new RoomRepository();

    public static void register(Javalin app) {
        app.get("/rooms", ctx -> {
            try {
                var rooms = roomRepo.findAll();
                JSONArray json = new JSONArray();

                for (Room room : rooms) {
                    JSONObject obj = new JSONObject();
                    obj.put("number", room.getNumber());
                    obj.put("type", room.getType().name());
                    obj.put("capacity", room.getCapacity());
                    obj.put("nightlyRate", Double.parseDouble(String.format("%.2f", room.getNightlyRate())));
                    json.put(obj);
                }

                ctx.json(json);
            } catch (Exception e) {
                ctx.status(500).json(new JSONObject().put("error", "INTERNAL_ERROR"));
            }
        });
    }
}
