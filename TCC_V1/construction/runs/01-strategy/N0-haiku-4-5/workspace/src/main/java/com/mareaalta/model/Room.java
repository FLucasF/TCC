package com.mareaalta.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Room(
    @JsonProperty("number") int number,
    @JsonProperty("type") String type,
    @JsonProperty("capacity") int capacity,
    @JsonProperty("nightlyRate") double nightlyRate
) {}
