package br.com.maralta.model;

import java.math.BigDecimal;

public class Room {
    private String number;
    private RoomType type;

    public Room() {}

    public Room(String number, RoomType type) {
        this.number = number;
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public int getCapacity() {
        return type.getCapacity();
    }

    public BigDecimal getNightlyRate() {
        return type.getNightlyRate();
    }
}
