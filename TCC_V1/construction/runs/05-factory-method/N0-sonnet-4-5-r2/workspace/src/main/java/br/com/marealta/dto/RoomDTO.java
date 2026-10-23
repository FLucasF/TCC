package br.com.marealta.dto;

import br.com.marealta.model.Room;

public record RoomDTO(
    String number,
    String type,
    int capacity,
    double nightlyRate
) {
    public static RoomDTO from(Room room) {
        return new RoomDTO(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getNightlyRate().doubleValue()
        );
    }
}
