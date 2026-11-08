package com.pousadamarealta.controller;

import com.pousadamarealta.dto.RoomResponse;
import com.pousadamarealta.model.Room;
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
        List<RoomResponse> rooms = roomRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        ctx.json(rooms);
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getNightlyRate()
        );
    }
}
