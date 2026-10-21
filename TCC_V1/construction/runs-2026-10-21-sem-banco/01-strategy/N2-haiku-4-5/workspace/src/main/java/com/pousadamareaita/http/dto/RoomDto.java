package com.pousadamareaita.http.dto;

import java.math.BigDecimal;

public class RoomDto {
    public int number;
    public String type;
    public int capacity;
    public double nightlyRate;

    public RoomDto(int number, String type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate.doubleValue();
    }
}
