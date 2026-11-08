package com.pousadamarealta.api.response;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class RoomResponse {
    private int number;
    private String type;
    private int capacity;
    @SerializedName("nightlyRate")
    private BigDecimal nightlyRate;

    public RoomResponse(int number, String type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public int getNumber() { return number; }
    public String getType() { return type; }
    public int getCapacity() { return capacity; }
    public BigDecimal getNightlyRate() { return nightlyRate; }
}
