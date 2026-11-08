package com.pousada.api;

import com.pousada.api.dto.RoomResponse;
import com.pousada.domain.Room;
import com.pousada.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.List;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        List<RoomResponse> response = rooms.stream()
            .map(room -> new RoomResponse(
                room.getNumber(),
                room.getType().name(),
                room.getCapacity(),
                room.getNightlyRate()
            ))
            .toList();
        ctx.json(response);
    }
}
