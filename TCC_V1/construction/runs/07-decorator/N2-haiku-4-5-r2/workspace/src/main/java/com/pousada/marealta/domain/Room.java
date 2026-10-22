package com.pousada.marealta.domain;

import java.math.BigDecimal;

public class Room {
    public enum Type {
        STANDARD, DELUXE, SUITE
    }

    private final int number;
    private final Type type;
    private final int capacity;
    private final BigDecimal nightlyRate;

    public Room(int number, Type type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public int getNumber() {
        return number;
    }

    public Type getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }
}
