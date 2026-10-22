package com.pousadamarealta.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;

public class RoomHandler {
    private final ReservationService reservationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoomHandler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void listRooms(Context ctx) {
        var rooms = reservationService.getAllRooms();
        var response = rooms.stream().map(room -> {
            Map<String, Object> roomMap = new HashMap<>();
            roomMap.put("number", room.number);
            roomMap.put("type", room.type);
            roomMap.put("capacity", room.capacity);
            roomMap.put("nightlyRate", formatMoney(room.nightlyRate));
            return roomMap;
        }).toList();

        ctx.json(response);
    }

    private String formatMoney(java.math.BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}
