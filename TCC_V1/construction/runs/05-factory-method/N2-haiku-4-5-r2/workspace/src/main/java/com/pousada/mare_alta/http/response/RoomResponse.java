package com.pousada.mare_alta.http.response;

import com.pousada.mare_alta.model.Room;
import com.pousada.mare_alta.util.NumberFormatter;

public class RoomResponse {
    public int number;
    public String type;
    public int capacity;
    public String nightlyRate;

    public static RoomResponse fromModel(Room room) {
        RoomResponse response = new RoomResponse();
        response.number = room.getNumber();
        response.type = room.getType().getValue();
        response.capacity = room.getCapacity();
        response.nightlyRate = NumberFormatter.formatCurrencyForJson(room.getNightlyRate());
        return response;
    }
}
