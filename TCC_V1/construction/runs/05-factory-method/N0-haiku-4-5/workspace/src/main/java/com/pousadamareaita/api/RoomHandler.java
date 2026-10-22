package com.pousadamareaita.api;

import io.javalin.http.Context;
import com.pousadamareaita.database.Database;
import com.pousadamareaita.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RoomHandler {
    private final RoomService roomService;
    private final ObjectMapper mapper = new ObjectMapper();

    public RoomHandler(Database db) {
        this.roomService = new RoomService(db);
    }

    public void getRooms(Context ctx) throws Exception {
        ctx.json(roomService.getAllRooms());
    }
}
