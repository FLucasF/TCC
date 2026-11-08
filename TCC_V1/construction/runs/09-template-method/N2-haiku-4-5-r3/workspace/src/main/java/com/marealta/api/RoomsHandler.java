package com.marealta.api;

import com.google.gson.Gson;
import com.marealta.db.RoomRepository;
import com.marealta.model.Room;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class RoomsHandler {
    private final RoomRepository roomRepository;
    private final Gson gson;

    public RoomsHandler(RoomRepository roomRepository, Gson gson) {
        this.roomRepository = roomRepository;
        this.gson = gson;
    }

    public void listRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        ctx.json(rooms);
    }
}
