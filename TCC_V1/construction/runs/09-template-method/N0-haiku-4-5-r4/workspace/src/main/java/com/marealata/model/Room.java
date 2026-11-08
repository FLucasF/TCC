package com.marealata.model;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class Room {
    @SerializedName("number")
    public int number;
    @SerializedName("type")
    public String type;
    @SerializedName("capacity")
    public int capacity;
    @SerializedName("nightlyRate")
    public BigDecimal nightlyRate;

    public Room(int number, String type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public Room() {}
}
