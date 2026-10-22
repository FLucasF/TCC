package com.pousada.marealta.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.List;

public class RoomsHandler extends Handler {
    private final RoomRepository roomRepository;

    public RoomsHandler(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getRooms(Context ctx) {
        try {
            List<Room> rooms = roomRepository.getAllRooms();
            JsonArray array = new JsonArray();

            for (Room room : rooms) {
                JsonObject obj = new JsonObject();
                obj.addProperty("number", room.getNumber());
                obj.addProperty("type", room.getType().getName());
                obj.addProperty("capacity", room.getCapacity());
                obj.addProperty("nightlyRate", room.getNightlyRate().doubleValue());
                array.add(obj);
            }

            ctx.json(array);
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }
}
