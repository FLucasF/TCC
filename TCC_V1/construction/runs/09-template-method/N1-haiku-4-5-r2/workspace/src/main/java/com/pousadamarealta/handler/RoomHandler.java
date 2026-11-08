package com.pousadamarealta.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import com.pousadamarealta.infrastructure.Database;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.model.Room;
import java.util.List;
import java.util.stream.Collectors;

public class RoomHandler {
    private RoomRepository roomRepository;

    public RoomHandler(Database database) {
        this.roomRepository = new RoomRepository(database);
    }

    public void listRooms(Context ctx) throws Exception {
        List<Room> rooms = roomRepository.findAll();
        List<RoomDto> dtos = rooms.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        ctx.json(dtos);
    }

    private RoomDto toDto(Room room) {
        return new RoomDto(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getNightlyRate()
        );
    }

    public static class RoomDto {
        public int number;
        public String type;
        public int capacity;
        public Object nightlyRate;

        public RoomDto(int number, String type, int capacity, Object nightlyRate) {
            this.number = number;
            this.type = type;
            this.capacity = capacity;
            this.nightlyRate = nightlyRate;
        }
    }
}
