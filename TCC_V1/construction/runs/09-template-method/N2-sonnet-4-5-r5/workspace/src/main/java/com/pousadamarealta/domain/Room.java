package com.pousadamarealta.domain;

public record Room(
    String number,
    RoomType type
) {
    public int capacity() {
        return type.getCapacity();
    }
}
