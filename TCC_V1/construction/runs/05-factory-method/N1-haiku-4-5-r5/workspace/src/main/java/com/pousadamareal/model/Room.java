package com.pousadamareal.model;

import java.math.BigDecimal;

public record Room(
    int number,
    RoomType type,
    int capacity,
    BigDecimal nightlyRate
) {}
