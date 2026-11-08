package com.pousada.marealta.model;

import java.math.BigDecimal;

public class RoomDto {
    public int number;
    public String type;
    public int capacity;
    public BigDecimal nightlyRate;

    public RoomDto(int number, String type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }
}
