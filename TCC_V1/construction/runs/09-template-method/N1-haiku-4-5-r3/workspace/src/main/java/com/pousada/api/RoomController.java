package com.pousada.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pousada.model.Room;
import com.pousada.repository.RoomRepository;
import io.javalin.http.Context;
import java.util.List;

public class RoomController {
    public static void getRooms(Context ctx) {
        List<Room> rooms = RoomRepository.findAll();

        JsonArray array = new JsonArray();
        for (Room room : rooms) {
            JsonObject obj = new JsonObject();
            obj.addProperty("number", room.getNumber());
            obj.addProperty("type", room.getType().getName());
            obj.addProperty("capacity", room.getCapacity());
            obj.addProperty("nightlyRate", room.getNightlyRate());
            array.add(obj);
        }

        ctx.json(array);
    }
}
