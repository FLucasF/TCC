package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RoomDTO(
    String number,
    String type,
    int capacity,
    @JsonProperty("nightlyRate") double nightlyRate
) {
}
