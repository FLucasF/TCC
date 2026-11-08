package com.pousadamarealta.controller;

import com.pousadamarealta.dto.RoomDto;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getRooms(Context ctx) {
        List<RoomDto> rooms = roomRepository.findAll().stream()
            .map(RoomDto::from)
            .collect(Collectors.toList());

        ctx.json(rooms);
    }
}
