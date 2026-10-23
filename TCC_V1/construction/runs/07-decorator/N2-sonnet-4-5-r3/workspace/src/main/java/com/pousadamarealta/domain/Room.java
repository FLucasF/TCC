package com.pousadamarealta.domain;

import java.math.BigDecimal;

public record Room(
    String number,
    RoomType type,
    int capacity,
    BigDecimal nightlyRate
) {}
