package com.pousada.api.dto;

import java.math.BigDecimal;

public class RoomResponse {
    private Integer number;
    private String type;
    private Integer capacity;
    private BigDecimal nightlyRate;

    public RoomResponse(Integer number, String type, Integer capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public Integer getNumber() {
        return number;
    }

    public String getType() {
        return type;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }
}
