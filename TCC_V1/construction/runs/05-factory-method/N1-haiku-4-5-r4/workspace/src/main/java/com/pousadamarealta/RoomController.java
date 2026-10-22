package com.pousadamarealta;

import io.javalin.Javalin;

public class RoomController {
    private final Database database;

    public RoomController(Database database) {
        this.database = database;
    }

    public void register(Javalin app) {
        app.get("/rooms", ctx -> {
            var rooms = new Room[] {
                new Room(101, RoomType.STANDARD),
                new Room(102, RoomType.STANDARD),
                new Room(201, RoomType.DELUXE),
                new Room(202, RoomType.DELUXE),
                new Room(301, RoomType.SUITE)
            };
            ctx.json(rooms);
        });
    }
}
