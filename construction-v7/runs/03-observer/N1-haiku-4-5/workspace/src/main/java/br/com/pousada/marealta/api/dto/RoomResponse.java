package br.com.pousada.marealta.api.dto;

import br.com.pousada.marealta.model.Room;

public record RoomResponse(
        int number,
        String type,
        int capacity,
        double nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.number(),
                room.type().getDisplayName(),
                room.capacity(),
                room.nightlyRate()
        );
    }
}
