package com.pousada.routes;

import com.pousada.database.Database;
import com.pousada.dto.RoomDTO;
import com.pousada.services.RoomService;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

public class RoomRoutes {
    private final RoomService roomService;

    public RoomRoutes(Database db) {
        this.roomService = new RoomService(db);
    }

    public void getRooms(Context ctx) {
        List<RoomDTO> rooms = roomService.getAllRooms().stream()
                .map(RoomDTO::new)
                .collect(Collectors.toList());
        ctx.json(rooms);
    }
}
