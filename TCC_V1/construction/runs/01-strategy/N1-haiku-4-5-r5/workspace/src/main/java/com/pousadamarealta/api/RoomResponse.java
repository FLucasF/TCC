package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.models.Room;
import java.math.BigDecimal;

public class RoomResponse {
    @JsonProperty("number")
    public final int number;

    @JsonProperty("type")
    public final String type;

    @JsonProperty("capacity")
    public final int capacity;

    @JsonProperty("nightlyRate")
    public final BigDecimal nightlyRate;

    public RoomResponse(Room room) {
        this.number = room.number;
        this.type = room.type;
        this.capacity = room.capacity;
        this.nightlyRate = room.nightlyRate;
    }
}
