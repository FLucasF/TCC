package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record RoomDTO(
    String number,
    String type,
    int capacity,
    @JsonProperty("nightlyRate") BigDecimal nightlyRate
) {}
