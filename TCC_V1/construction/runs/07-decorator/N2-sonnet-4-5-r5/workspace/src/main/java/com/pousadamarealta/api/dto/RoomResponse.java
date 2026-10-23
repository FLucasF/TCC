package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RoomResponse(
    @JsonProperty("number") String number,
    @JsonProperty("type") String type,
    @JsonProperty("capacity") int capacity,
    @JsonProperty("nightlyRate") double nightlyRate
) {}
