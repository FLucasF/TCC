package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Room;

import java.math.BigDecimal;

public class RoomResponse {
    private final int number;
    private final String type;
    private final int capacity;
    private final BigDecimal nightlyRate;

    public RoomResponse(Room room) {
        this.number = room.getNumber();
        this.type = room.getType().name();
        this.capacity = room.getCapacity();
        this.nightlyRate = room.getNightlyRate();
    }

    public int getNumber() {
        return number;
    }

    public String getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    @JsonProperty("nightlyRate")
    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }
}
