package com.pousada.models;

import java.math.BigDecimal;

public class Room {
    public enum RoomType {
        STANDARD, DELUXE, SUITE
    }

    private final int number;
    private final RoomType type;
    private final int capacity;
    private final BigDecimal nightlyRate;

    public Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
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

    public static Room[] FIXED_ROOMS = new Room[] {
        new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
        new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
        new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
        new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
        new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90"))
    };
}
