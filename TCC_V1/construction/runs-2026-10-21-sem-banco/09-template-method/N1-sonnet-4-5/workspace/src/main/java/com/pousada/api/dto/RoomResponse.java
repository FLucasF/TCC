package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Room;

import java.math.BigDecimal;

public class RoomResponse {
    private String number;
    private String type;
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

    public void setNumber(String number) {
        this.number = number;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }

    public void setNightlyRate(BigDecimal nightlyRate) {
        this.nightlyRate = nightlyRate;
    }
}
