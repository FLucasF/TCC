package com.marealta.domain.model;

import java.math.BigDecimal;

public record Room(
    String number,
    RoomType type
) {
    public int capacity() {
        return type.getCapacity();
    }

    public BigDecimal nightlyRate() {
        return type.getNightlyRate();
    }
}
