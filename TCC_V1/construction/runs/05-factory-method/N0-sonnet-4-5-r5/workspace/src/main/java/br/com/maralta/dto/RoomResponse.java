package br.com.maralta.dto;

import br.com.maralta.model.Room;
import br.com.maralta.model.RoomType;

import java.math.BigDecimal;

public class RoomResponse {
    private String number;
    private RoomType type;
    private int capacity;
    private BigDecimal nightlyRate;

    public static RoomResponse from(Room room) {
        RoomResponse response = new RoomResponse();
        response.number = room.getNumber();
        response.type = room.getType();
        response.capacity = room.getCapacity();
        response.nightlyRate = room.getNightlyRate();
        return response;
    }

    public String getNumber() {
        return number;
    }

    public RoomType getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }
}
