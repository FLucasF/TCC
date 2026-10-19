package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.dto.RoomDTO;
import com.pousadamarealta.api.mapper.RoomMapper;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getAllRooms(Context ctx) {
        List<RoomDTO> rooms = roomRepository.findAll().stream()
            .map(RoomMapper::toDTO)
            .collect(Collectors.toList());
        ctx.json(rooms);
    }
}
