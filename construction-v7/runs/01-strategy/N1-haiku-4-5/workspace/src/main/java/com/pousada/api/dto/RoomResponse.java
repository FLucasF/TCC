package com.pousada.api.dto;

import java.math.BigDecimal;

public class RoomResponse {
    public int number;
    public String type;
    public int capacity;
    public BigDecimal nightlyRate;

    public RoomResponse(int number, String type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }
}
