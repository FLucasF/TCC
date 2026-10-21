package com.pousada.marealta.model;

import java.math.BigDecimal;

public class Room {
    public final int number;
    public final RoomType type;

    public Room(int number, RoomType type) {
        this.number = number;
        this.type = type;
    }

    public int getCapacity() {
        return type.capacity;
    }

    public BigDecimal getNightlyRate() {
        return type.nightlyRate;
    }
}
