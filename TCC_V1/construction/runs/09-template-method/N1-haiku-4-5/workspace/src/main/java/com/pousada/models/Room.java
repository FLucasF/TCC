package com.pousada.models;

import java.math.BigDecimal;

public class Room {
    private int number;
    private RoomType type;
    private int capacity;
    private BigDecimal nightlyRate;

    public Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public int getNumber() {
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
