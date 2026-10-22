package com.pousada.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pousada.dao.RoomDAO;
import com.pousada.model.Room;
import io.javalin.http.Context;

import java.util.List;

public class RoomHandler {
    private static final RoomDAO roomDAO = new RoomDAO();

    public static void getRooms(Context ctx) {
        List<Room> rooms = roomDAO.findAll();

        JsonArray jsonArray = new JsonArray();
        for (Room room : rooms) {
            JsonObject json = new JsonObject();
            json.addProperty("number", room.getNumber());
            json.addProperty("type", room.getType());
            json.addProperty("capacity", room.getCapacity());
            json.addProperty("nightlyRate", room.getNightlyRate());
            jsonArray.add(json);
        }

        ctx.json(jsonArray);
    }
}
