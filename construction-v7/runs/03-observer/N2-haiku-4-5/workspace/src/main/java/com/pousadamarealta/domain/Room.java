package com.pousadamarealta.domain;

import java.math.BigDecimal;

public record Room(
    int number,
    RoomType type,
    int capacity,
    BigDecimal nightly
) {
    public Room(int number, RoomType type) {
        this(number, type, type.capacity, type.nightly);
    }
}
