package com.pousada.handler;

import com.google.gson.Gson;
import com.pousada.domain.Room;
import com.pousada.service.RoomService;
import io.javalin.http.Context;

import java.util.List;

public class RoomHandler {
    private static final Gson gson = new Gson();

    public static void listRooms(Context ctx) {
        List<Room> rooms = RoomService.getAllRooms();
        ctx.json(rooms);
    }
}
