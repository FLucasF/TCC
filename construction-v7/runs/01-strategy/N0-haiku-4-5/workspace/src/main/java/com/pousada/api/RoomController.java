package com.pousada.api;

import com.pousada.domain.Room;
import com.pousada.service.RoomService;
import io.javalin.http.Context;
import java.util.List;
import java.util.stream.Collectors;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getAllRooms(Context ctx) {
        List<Room> rooms = roomService.getAllRooms();
        List<RoomResponse> responses = rooms.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        ctx.json(responses);
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getNumber(),
                room.getType(),
                room.getCapacity(),
                room.getNightlyRate()
        );
    }

    public static class RoomResponse {
        public final int number;
        public final String type;
        public final int capacity;
        public final Object nightlyRate;

        public RoomResponse(int number, String type, int capacity, Object nightlyRate) {
            this.number = number;
            this.type = type;
            this.capacity = capacity;
            this.nightlyRate = nightlyRate;
        }
    }
}
