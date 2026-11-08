package com.pousada.api;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.model.Room;
import com.pousada.repository.RoomRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomController {
  private static final RoomRepository roomRepository = new RoomRepository();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static void routes(Javalin app) {
    app.get("/rooms", ctx -> {
      List<Room> rooms = roomRepository.findAll();
      ctx.json(rooms.stream()
          .map(room -> Map.of(
              "number", room.getNumber(),
              "type", room.getType().toString(),
              "capacity", room.getCapacity(),
              "nightlyRate", room.getNightlyRate()
          ))
          .toList());
    });
  }
}
