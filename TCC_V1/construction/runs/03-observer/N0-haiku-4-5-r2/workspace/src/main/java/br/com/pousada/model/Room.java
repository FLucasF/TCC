package br.com.pousada.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Room {
    @JsonProperty("number")
    public int number;

    @JsonProperty("type")
    public RoomType type;

    @JsonProperty("capacity")
    public int capacity;

    @JsonProperty("nightlyRate")
    public double nightlyRate;

    public Room(int number, RoomType type, int capacity, double nightlyRate) {
        this.number = number;
        this.type = type;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }
}
