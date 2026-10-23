package br.com.marealto.dto;

import br.com.marealto.model.Room;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record RoomResponse(
        String number,
        String type,
        int capacity,
        @JsonProperty("nightlyRate") BigDecimal nightlyRate
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getNumber(),
                room.getType().name(),
                room.getCapacity(),
                room.getNightlyRate()
        );
    }
}
