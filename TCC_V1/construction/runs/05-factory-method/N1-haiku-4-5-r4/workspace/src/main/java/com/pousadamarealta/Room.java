package com.pousadamarealta;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Room {
    public final int number;
    @JsonProperty("type")
    public final String type;
    public final int capacity;

    @JsonProperty("nightlyRate")
    public final double nightlyRate;

    public Room(int number, RoomType roomType) {
        this.number = number;
        this.type = roomType.type;
        this.capacity = roomType.capacity;
        this.nightlyRate = roomType.nightlyRate;
    }
}
