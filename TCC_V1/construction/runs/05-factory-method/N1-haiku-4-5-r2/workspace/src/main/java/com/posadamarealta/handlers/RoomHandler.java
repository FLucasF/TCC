package com.posadamarealta.handlers;

import com.posadamarealta.repository.RoomRepository;
import io.javalin.http.Context;
import javax.sql.DataSource;

public class RoomHandler {
    private final RoomRepository roomRepo;

    public RoomHandler(DataSource ds) {
        this.roomRepo = new RoomRepository(ds);
    }

    public void list(Context ctx) {
        ctx.json(roomRepo.findAll());
    }
}
