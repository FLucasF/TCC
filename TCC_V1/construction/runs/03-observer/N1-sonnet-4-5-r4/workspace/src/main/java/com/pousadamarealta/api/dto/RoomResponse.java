package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.model.Room;

import java.math.BigDecimal;

public class RoomResponse {
    @JsonProperty("number")
    private String number;

    @JsonProperty("type")
    private String type;

    @JsonProperty("capacity")
    private int capacity;

    @JsonProperty("nightlyRate")
    private BigDecimal nightlyRate;

    public RoomResponse(Room room) {
        this.number = room.getNumber();
        this.type = room.getType().name();
        this.capacity = room.getCapacity();
        this.nightlyRate = room.getNightlyRate();
    }

    public String getNumber() {
        return number;
    }

    public String getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }
}
