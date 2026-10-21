package com.pousadamarealta.api;

import com.pousadamarealta.service.RoomService;
import io.javalin.http.Context;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getAllRooms(Context ctx) {
        var rooms = roomService.getAllRooms();
        var response = rooms.stream().map(r -> new Object() {
            public final int number = r.number();
            public final String type = r.type().name;
            public final int capacity = r.capacity();
            public final double nightlyRate = r.nightlyRate();
        }).toList();
        ctx.json(response);
    }
}
