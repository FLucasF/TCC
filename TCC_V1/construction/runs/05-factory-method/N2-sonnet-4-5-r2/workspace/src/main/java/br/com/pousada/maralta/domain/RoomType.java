package br.com.pousada.maralta.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RoomType {
    STANDARD(249.90, 2),
    DELUXE(389.90, 3),
    SUITE(579.90, 4);

    private final double baseRate;
    private final int capacity;

    RoomType(double baseRate, int capacity) {
        this.baseRate = baseRate;
        this.capacity = capacity;
    }

    public double getBaseRate() {
        return baseRate;
    }

    public int getCapacity() {
        return capacity;
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}
