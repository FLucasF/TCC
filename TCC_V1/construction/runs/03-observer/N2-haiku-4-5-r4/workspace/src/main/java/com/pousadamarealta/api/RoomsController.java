package com.pousadamarealta.api;

import com.pousadamarealta.services.Database;
import io.javalin.Javalin;
import java.util.stream.Collectors;

public class RoomsController {
    public static void register(Javalin app, Database database) {
        app.get("/rooms", ctx -> {
            var rooms = database.getAllRooms();
            var json = rooms.stream().map(room -> new Object() {
                public final int number = room.getNumber();
                public final String type = room.getType().getValue();
                public final int capacity = room.getCapacity();
                public final double nightlyRate = room.getNightlyRate().doubleValue();
            }).collect(Collectors.toList());
            ctx.json(json);
        });
    }
}
