package com.pousadamarealta.dto;

public class RoomDTO {
    private String number;
    private String type;
    private int capacity;
    private double nightlyRate;

    public RoomDTO() {
    }

    public RoomDTO(String number, String type, int capacity, double nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }

    public void setNightlyRate(double nightlyRate) {
        this.nightlyRate = nightlyRate;
    }
}
