package com.pousadamarealta.api;

import com.pousadamarealta.infrastructure.RoomRepository;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomController {
  public static void listRooms(Context ctx) throws Exception {
    var rooms = RoomRepository.findAll();
    var roomDtos = rooms.stream()
        .map(room -> {
          Map<String, Object> dto = new HashMap<>();
          dto.put("number", room.number);
          dto.put("type", room.type.value);
          dto.put("capacity", room.capacity);
          dto.put("nightlyRate", room.nightlyRate);
          return dto;
        })
        .toList();
    ctx.json(roomDtos);
  }
}
