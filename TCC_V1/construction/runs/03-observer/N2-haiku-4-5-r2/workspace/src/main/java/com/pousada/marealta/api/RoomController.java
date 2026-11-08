package com.pousada.marealta.api;

import com.pousada.marealta.repository.RoomRepository;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomController {
  private static final Logger logger = LoggerFactory.getLogger(RoomController.class);

  public static void getRooms(Context ctx) {
    try {
      var rooms = RoomRepository.findAll();
      var result = rooms.stream().map(room -> new Object() {
        public int number = room.getNumber();
        public String type = room.getType();
        public int capacity = room.getCapacity();
        public double nightlyRate = room.getNightlyRate().doubleValue();
      }).toList();
      ctx.json(result);
    } catch (Exception e) {
      logger.error("Error fetching rooms", e);
      ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Failed to fetch rooms"));
    }
  }
}
