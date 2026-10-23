package br.com.marealta.controller;

import br.com.marealta.dto.RoomDTO;
import br.com.marealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.List;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getRooms(Context ctx) {
        List<RoomDTO> rooms = roomRepository.findAll().stream()
                .map(RoomDTO::from)
                .toList();
        ctx.json(rooms);
    }
}
