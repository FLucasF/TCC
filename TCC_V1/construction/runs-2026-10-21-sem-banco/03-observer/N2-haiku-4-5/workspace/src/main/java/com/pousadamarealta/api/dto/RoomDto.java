package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RoomDto {
    @JsonProperty("number")
    public int number;

    @JsonProperty("type")
    public String type;

    @JsonProperty("capacity")
    public int capacity;

    @JsonProperty("nightlyRate")
    public double nightlyRate;

    public RoomDto(int number, String type, int capacity, double nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }
}
