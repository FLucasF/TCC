package com.pousada.marealta.infrastructure.web.handler;

import com.pousada.marealta.domain.repository.RoomRepository;
import com.pousada.marealta.infrastructure.web.dto.RoomDTO;
import io.javalin.http.Context;

import java.util.stream.Collectors;

public class RoomsHandler {
    private final RoomRepository roomRepository;

    public RoomsHandler(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void listRooms(Context ctx) {
        var rooms = roomRepository.findAll()
                .stream()
                .map(RoomDTO::fromDomain)
                .collect(Collectors.toList());

        ctx.json(rooms);
    }
}
