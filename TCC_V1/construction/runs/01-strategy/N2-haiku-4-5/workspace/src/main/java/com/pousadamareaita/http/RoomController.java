package com.pousadamareaita.http;

import com.pousadamareaita.http.dto.RoomDto;
import com.pousadamareaita.storage.RoomRepository;
import io.javalin.Javalin;
import com.pousadamareaita.model.Room;
import java.util.ArrayList;
import java.util.List;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void registerRoutes(Javalin app) {
        app.get("/rooms", ctx -> {
            List<RoomDto> rooms = new ArrayList<>();
            for (Room room : roomRepository.findAll()) {
                rooms.add(new RoomDto(room.getNumber(), room.getType().toString(), room.getCapacity(), room.getNightlyRate()));
            }
            ctx.json(rooms);
        });
    }
}
