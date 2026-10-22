package com.pousada.marealta.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.infrastructure.RoomRepository;
import io.javalin.http.Context;
import java.util.List;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getRooms(Context ctx) throws Exception {
        List<Room> rooms = roomRepository.findAll();
        JsonArray jsonArray = new JsonArray();

        for (Room room : rooms) {
            JsonObject obj = new JsonObject();
            obj.addProperty("number", room.getNumber());
            obj.addProperty("type", room.getType().getLabel());
            obj.addProperty("capacity", room.getCapacity());
            obj.addProperty("nightlyRate", room.getNightlyRate().doubleValue());
            jsonArray.add(obj);
        }

        ctx.json(jsonArray);
    }
}
