package com.pousadamarealta.model;

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

    public String getTypeString() {
        return switch (type) {
            case STANDARD -> "STANDARD";
            case DELUXE -> "DELUXE";
            case SUITE -> "SUITE";
        };
    }
}
