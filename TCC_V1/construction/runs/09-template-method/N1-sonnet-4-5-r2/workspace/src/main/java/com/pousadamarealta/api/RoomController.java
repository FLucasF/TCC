package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getRooms(Context ctx) {
        List<RoomDto> rooms = roomRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        ctx.json(rooms);
    }

    private RoomDto toDto(Room room) {
        return new RoomDto(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getNightlyRate()
        );
    }

    record RoomDto(
        int number,
        String type,
        int capacity,
        @JsonProperty("nightlyRate") BigDecimal nightlyRate
    ) {}
}
