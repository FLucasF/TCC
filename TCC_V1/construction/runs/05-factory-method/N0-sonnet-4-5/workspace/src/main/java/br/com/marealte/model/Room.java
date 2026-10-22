package br.com.marealte.model;

import java.math.BigDecimal;

public class Room {
    private String number;
    private RoomType type;
    private int capacity;
    private BigDecimal nightlyRate;

    public Room() {}

    public Room(String number, RoomType type, int capacity, BigDecimal nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public BigDecimal getNightlyRate() { return nightlyRate; }
    public void setNightlyRate(BigDecimal nightlyRate) { this.nightlyRate = nightlyRate; }
}
