package com.pousada.marealta.domain;

import java.math.BigDecimal;

public class Room {
    private final String number;
    private final RoomType type;
    private final int capacity;
    private final BigDecimal nightlyRate;

    public Room(String number, RoomType type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public String getNumber() {
        return number;
    }

    public RoomType getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }
}
