package com.pousadamareaalta.api.dto;

import java.math.BigDecimal;

public record RoomDTO(
    int number,
    String type,
    int capacity,
    BigDecimal nightlyRate
) {
}
