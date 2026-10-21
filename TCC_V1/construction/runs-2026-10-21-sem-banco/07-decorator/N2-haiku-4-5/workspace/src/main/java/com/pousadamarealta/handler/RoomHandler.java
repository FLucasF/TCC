package com.pousadamarealta.handler;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RoomHandler {
    private final RoomRepository roomRepo;

    public RoomHandler(RoomRepository roomRepo) {
        this.roomRepo = roomRepo;
    }

    public void listRooms(Context ctx) {
        List<Map<String, Object>> rooms = new ArrayList<>();
        for (Room room : roomRepo.findAll()) {
            Map<String, Object> roomMap = new LinkedHashMap<>();
            roomMap.put("number", room.number);
            roomMap.put("type", room.getTypeString());
            roomMap.put("capacity", room.capacity);
            roomMap.put("nightlyRate", room.nightlyRate);
            rooms.add(roomMap);
        }
        ctx.json(rooms);
    }
}
