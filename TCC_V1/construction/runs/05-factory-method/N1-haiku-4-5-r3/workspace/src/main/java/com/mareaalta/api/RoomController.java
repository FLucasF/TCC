package com.mareaalta.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mareaalta.domain.Room;
import com.mareaalta.infrastructure.RoomRepository;
import io.javalin.http.Context;
import java.util.*;

public class RoomController {
    private static final Gson gson = new Gson();

    public static void getRooms(Context ctx) throws Exception {
        List<Room> rooms = RoomRepository.findAll();

        List<JsonObject> roomsJson = new ArrayList<>();
        for (Room room : rooms) {
            JsonObject obj = new JsonObject();
            obj.addProperty("number", room.getNumber());
            obj.addProperty("type", room.getType().name());
            obj.addProperty("capacity", room.getCapacity());
            obj.addProperty("nightlyRate", room.getNightlyRate().doubleValue());
            roomsJson.add(obj);
        }

        ctx.json(roomsJson);
    }
}
