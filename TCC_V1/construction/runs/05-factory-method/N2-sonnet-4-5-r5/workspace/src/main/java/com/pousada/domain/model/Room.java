package com.pousada.domain.model;

import java.math.BigDecimal;

public final class Room {
    private final String number;
    private final RoomType type;

    public Room(String number, RoomType type) {
        this.number = number;
        this.type = type;
    }

    public String number() {
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
