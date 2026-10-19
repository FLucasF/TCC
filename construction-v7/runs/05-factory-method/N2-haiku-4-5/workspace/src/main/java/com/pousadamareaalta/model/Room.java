package com.pousadamareaalta.model;

import java.math.BigDecimal;

public record Room(
    int number,
    RoomType type,
    int capacity,
    BigDecimal nightlyRate
) {
    public enum RoomType {
        STANDARD, DELUXE, SUITE
    }
}
