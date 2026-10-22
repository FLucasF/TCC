package com.pousadamareaalta.handlers;

import com.pousadamareaalta.repositories.RoomRepository;
import io.javalin.http.Context;

public class RoomHandler {
    private final RoomRepository roomRepository = new RoomRepository();

    public void list(Context ctx) {
        try {
            var rooms = roomRepository.findAll();
            var response = rooms.stream().map(r -> new Object() {
                public final int number = r.number;
                public final String type = r.type;
                public final int capacity = r.capacity;
                public final double nightlyRate = r.nightlyRate;
            }).toList();

            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500).json(new Object() {
                public final String error = "INTERNAL_ERROR";
                public final String message = e.getMessage();
            });
        }
    }
}
