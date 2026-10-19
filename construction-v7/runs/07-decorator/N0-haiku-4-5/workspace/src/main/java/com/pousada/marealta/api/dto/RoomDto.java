package com.pousada.marealta.api.dto;

import java.math.BigDecimal;

public record RoomDto(
    int number,
    String type,
    int capacity,
    BigDecimal nightlyRate
) {}
