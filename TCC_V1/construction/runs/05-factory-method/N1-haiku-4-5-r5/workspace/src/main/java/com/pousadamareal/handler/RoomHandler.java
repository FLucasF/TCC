package com.pousadamareal.handler;

import com.pousadamareal.model.Room;
import com.pousadamareal.repository.RoomRepository;
import com.pousadamareal.util.JsonUtils;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.List;

public class RoomHandler {

    public static void listRooms(Context ctx) {
        try {
            List<Room> rooms = RoomRepository.findAll();
            ctx.json(rooms.stream().map(room -> JsonUtils.map(
                "number", room.number(),
                "type", room.type().name(),
                "capacity", room.capacity(),
                "nightlyRate", room.nightlyRate()
            )).toList());
        } catch (SQLException e) {
            ctx.status(500).json(JsonUtils.map("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }
}
