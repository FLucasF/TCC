package com.pousadamareaita;

import com.pousadamareaita.model.Room;
import com.pousadamareaita.repository.RoomRepository;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoomHandler {
    private static final RoomRepository roomRepo = new RoomRepository();

    public static void listRooms(Context ctx) {
        List<Room> rooms = roomRepo.findAll();

        List<java.util.Map<String, Object>> response = rooms.stream()
            .map(room -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("number", room.getNumber());
                m.put("type", room.getType());
                m.put("capacity", room.getCapacity());
                m.put("nightlyRate", room.getNightlyRate());
                return m;
            })
            .collect(Collectors.toList());

        ctx.json(response);
    }
}
