package com.pousadamarealta.domain;

import java.math.BigDecimal;

public record Room(String number, RoomType type) {
    public int capacity() {
        return type.capacity();
    }

    public BigDecimal nightlyRate() {
        return type.nightlyRate();
    }
}
