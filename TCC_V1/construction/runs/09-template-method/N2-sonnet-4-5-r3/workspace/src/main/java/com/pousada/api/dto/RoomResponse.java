package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record RoomResponse(
    String number,
    String type,
    int capacity,
    @JsonProperty("nightlyRate") BigDecimal nightlyRate
) {
}
