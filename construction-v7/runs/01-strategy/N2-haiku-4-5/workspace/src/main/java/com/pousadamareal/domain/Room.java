package com.pousadamareal.domain;

import java.math.BigDecimal;

public class Room {
    public final int number;
    public final RoomType type;
    public final int capacity;
    public final BigDecimal nightlyRate;

    public Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public enum RoomType {
        STANDARD, DELUXE, SUITE
    }
}
