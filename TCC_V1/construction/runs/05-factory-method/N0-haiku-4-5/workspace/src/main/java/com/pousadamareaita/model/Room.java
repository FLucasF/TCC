package com.pousadamareaita.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class Room {
    @JsonProperty("number")
    private int number;

    @JsonProperty("type")
    private String type;

    @JsonProperty("capacity")
    private int capacity;

    @JsonProperty("nightlyRate")
    private BigDecimal nightlyRate;

    public Room(int number, String type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public int getNumber() {
        return number;
    }

    public String getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }
}
