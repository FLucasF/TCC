package com.pousadamarealta.domain;

import java.math.BigDecimal;

public final class Room {
    private final int number;
    private final RoomType type;

    public Room(int number, RoomType type) {
        this.number = number;
        this.type = type;
    }

    public int number() {
        return number;
    }

    public RoomType type() {
        return type;
    }

    public int capacity() {
        return type.capacity();
    }

    public BigDecimal nightlyRate() {
        return type.nightlyRate();
    }
}
