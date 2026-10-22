package com.pousada.model;

import java.math.BigDecimal;

public class Room {
    private int number;
    private String type;
    private int capacity;
    private BigDecimal nightlyRate;

    public Room(int number, String type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
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

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }
}
