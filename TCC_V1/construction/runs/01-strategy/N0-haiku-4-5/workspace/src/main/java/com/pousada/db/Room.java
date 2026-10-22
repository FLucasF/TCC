package com.pousada.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class Room {
    @JsonProperty("number")
    public int number;

    @JsonProperty("type")
    public String type;

    @JsonProperty("capacity")
    public int capacity;

    @JsonProperty("nightlyRate")
    public BigDecimal nightlyRate;

    public Room(int number, String type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }
}
