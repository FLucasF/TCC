package com.pousada.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pousada.database.RoomRepository;
import com.pousada.model.Room;
import io.javalin.http.Context;
import java.util.List;
import java.util.stream.Collectors;

public class RoomsEndpoint {
  private static final Gson gson = new Gson();

  public static void listRooms(Context ctx) {
    List<Room> rooms = RoomRepository.findAll();
    List<JsonObject> roomsJson =
        rooms.stream()
            .map(
                room -> {
                  JsonObject obj = new JsonObject();
                  obj.addProperty("number", room.number);
                  obj.addProperty("type", room.type);
                  obj.addProperty("capacity", room.capacity);
                  obj.addProperty("nightlyRate", room.nightlyRate);
                  return obj;
                })
            .collect(Collectors.toList());

    ctx.json(roomsJson);
  }
}
