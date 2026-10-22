package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.RoomDTO;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;
import java.util.List;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getAllRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        List<RoomDTO> dtos = rooms.stream()
            .map(RoomDTO::from)
            .toList();
        ctx.json(dtos);
    }
}
