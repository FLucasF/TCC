package com.pousada.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.domain.Room;
import com.pousada.repository.RoomRepository;
import io.javalin.Javalin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoomsApiHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void register(Javalin app, RoomRepository roomRepo) {
        app.get("/rooms", ctx -> {
            Map<Integer, Room> allRooms = roomRepo.findAll();
            List<Map<String, Object>> response = new ArrayList<>();

            List<Integer> roomNumbers = new ArrayList<>(allRooms.keySet());
            java.util.Collections.sort(roomNumbers);

            for (int roomNumber : roomNumbers) {
                Room room = allRooms.get(roomNumber);
                Map<String, Object> roomMap = new java.util.LinkedHashMap<>();
                roomMap.put("number", room.getNumber());
                roomMap.put("type", room.getType().toString());
                roomMap.put("capacity", room.getCapacity());
                roomMap.put("nightlyRate", room.getNightlyRate().doubleValue());
                response.add(roomMap);
            }

            ctx.json(response);
        });
    }
}
