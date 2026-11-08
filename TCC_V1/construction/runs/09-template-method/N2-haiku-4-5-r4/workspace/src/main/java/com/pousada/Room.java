package com.pousada;

public class Room {
    public int number;
    public String type;
    public int capacity;
    public double nightlyRate;

    public Room(int number, String type, int capacity, double nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }
}
