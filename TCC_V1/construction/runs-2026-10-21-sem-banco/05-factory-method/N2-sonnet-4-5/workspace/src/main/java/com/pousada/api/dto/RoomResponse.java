package com.pousada.api.dto;

import java.math.BigDecimal;

public class RoomResponse {
    private String number;
    private String type;
    private int capacity;
    private BigDecimal nightlyRate;

    public RoomResponse(String number, String type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
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
