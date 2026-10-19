package com.pousadamareal.api;

import com.pousadamareal.domain.Room;
import com.pousadamareal.service.DataStore;
import io.javalin.http.Context;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RoomController {
    private final DataStore dataStore;

    public RoomController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void getRooms(Context ctx) {
        List<RoomResponse> rooms = dataStore.getAllRooms().stream()
                .sorted(Comparator.comparingInt(r -> r.number))
                .map(room -> new RoomResponse(
                        room.number,
                        room.type.name(),
                        room.capacity,
                        Math.round(room.nightlyRate.doubleValue() * 100.0) / 100.0
                ))
                .collect(Collectors.toList());

        ctx.json(rooms);
    }

    public record RoomResponse(int number, String type, int capacity, double nightlyRate) {}
}
