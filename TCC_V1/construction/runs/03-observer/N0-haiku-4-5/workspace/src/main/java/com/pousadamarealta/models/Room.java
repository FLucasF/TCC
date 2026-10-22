package com.pousadamarealta.models;

import java.math.BigDecimal;

public class Room {
    public int number;
    public RoomType type;
    public int capacity;
    public BigDecimal nightlyRate;

    public Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }
}
