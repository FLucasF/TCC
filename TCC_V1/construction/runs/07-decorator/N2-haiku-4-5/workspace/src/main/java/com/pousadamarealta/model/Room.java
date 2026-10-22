package com.pousadamarealta.model;

import java.math.BigDecimal;

public record Room(
    Integer number,
    String type,
    Integer capacity,
    BigDecimal nightlyRate
) {
    public static Room of(Integer number, String type, Integer capacity, BigDecimal nightlyRate) {
        return new Room(number, type, capacity, nightlyRate);
    }
}
