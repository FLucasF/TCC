package com.marealta.api.handlers;

import io.javalin.http.Context;
import com.marealta.service.RoomService;
import com.marealta.domain.Room;
import java.util.List;
import java.util.stream.Collectors;

public class RoomHandler {
    public static void list(Context ctx) throws Exception {
        List<Room> rooms = RoomService.getAllRooms();

        var response = rooms.stream().map(room -> new Object() {
            public int number = room.number;
            public String type = room.type;
            public int capacity = room.capacity;
            public double nightlyRate = room.nightlyRate.doubleValue();
        }).collect(Collectors.toList());

        ctx.json(response);
    }
}
