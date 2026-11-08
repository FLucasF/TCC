package com.pousada.model;

import java.math.BigDecimal;

public class Room {
    public final int number;
    public final String type;
    public final int capacity;
    public final BigDecimal nightlyRate;

    public Room(int number, String type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }
}
