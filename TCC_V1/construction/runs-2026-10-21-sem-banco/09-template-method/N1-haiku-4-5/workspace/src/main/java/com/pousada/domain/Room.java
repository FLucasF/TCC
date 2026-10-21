package com.pousada.domain;

import java.math.BigDecimal;

public class Room {
    private final int number;
    private final RoomType type;
    private final int capacity;
    private final BigDecimal nightlyRate;

    public Room(int number, RoomType type) {
        this.number = number;
        this.type = type;
        this.capacity = type.getCapacity();
        this.nightlyRate = type.getNightlyRate();
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
