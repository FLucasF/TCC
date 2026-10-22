package br.pousada.marealta.model;

import com.google.gson.annotations.SerializedName;

public class Room {
    private int number;
    private RoomType type;
    private int capacity;
    @SerializedName("nightlyRate")
    private double nightlyRate;

    public Room(int number, RoomType type, int capacity, double nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public int getNumber() { return number; }
    public RoomType getType() { return type; }
    public int getCapacity() { return capacity; }
    public double getNightlyRate() { return nightlyRate; }
}
