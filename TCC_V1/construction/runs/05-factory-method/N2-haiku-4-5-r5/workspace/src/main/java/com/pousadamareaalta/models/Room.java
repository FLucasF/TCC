package com.pousadamareaalta.models;

public class Room {
    public final int number;
    public final String type;
    public final int capacity;
    public final double nightlyRate;

    public Room(int number, String type, int capacity, double nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }
}
