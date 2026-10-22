package com.pousada.api.handler;

import com.pousada.api.JsonUtil;
import com.pousada.domain.Room;
import com.pousada.repository.RoomRepository;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;

public class RoomHandler {
    public static void register(Javalin app) {
        app.get("/rooms", ctx -> {
            var rooms = RoomRepository.getInstance().findAll();
            JSONArray array = new JSONArray();
            for (Room room : rooms) {
                JSONObject obj = new JSONObject();
                obj.put("number", room.number());
                obj.put("type", room.type().name());
                obj.put("capacity", room.capacity());
                obj.put("nightlyRate", JsonUtil.formatDecimal(room.nightlyRate()));
                array.put(obj);
            }
            ctx.json(array.toList());
        });
    }
}
