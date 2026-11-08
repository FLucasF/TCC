package br.marealta.api;

import br.marealta.service.RoomService;
import io.javalin.http.Context;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getRooms(Context ctx) {
        ctx.json(roomService.getAllRooms().stream()
            .map(room -> new Object() {
                public final int number = room.getNumber();
                public final String type = room.getType().name();
                public final int capacity = room.getCapacity();
                public final double nightlyRate = room.getNightlyRate().doubleValue();
            })
            .toList());
    }
}
