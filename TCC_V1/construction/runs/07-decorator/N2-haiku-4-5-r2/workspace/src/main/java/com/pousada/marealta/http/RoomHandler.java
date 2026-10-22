package com.pousada.marealta.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.List;

public class RoomHandler {
    private final RoomRepository roomRepository;
    private final Gson gson = new Gson();

    public RoomHandler(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void listRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        List<JsonObject> response = rooms.stream()
            .map(this::toJson)
            .toList();
        ctx.json(response);
    }

    private JsonObject toJson(Room room) {
        JsonObject json = new JsonObject();
        json.addProperty("number", room.getNumber());
        json.addProperty("type", room.getType().name());
        json.addProperty("capacity", room.getCapacity());
        json.addProperty("nightlyRate", new java.math.BigDecimal(String.format("%.2f", room.getNightlyRate())));
        return json;
    }
}
