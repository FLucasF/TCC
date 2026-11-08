package com.pousadamarealta.domain.model;

import java.math.BigDecimal;

public class Room {
    private final String number;
    private final RoomType type;

    public Room(String number, RoomType type) {
        this.number = number;
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public RoomType getType() {
        return type;
    }

    public int getCapacity() {
        return type.getCapacity();
    }

    public BigDecimal getNightlyRate() {
        return type.getNightlyRate();
    }
}
