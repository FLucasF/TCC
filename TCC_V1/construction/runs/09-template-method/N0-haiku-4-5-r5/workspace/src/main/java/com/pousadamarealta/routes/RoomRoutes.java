package com.pousadamarealta.routes;

import io.javalin.http.Context;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.util.CurrencyFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomRoutes {
    private final RoomRepository roomRepository = new RoomRepository();

    public void listRooms(Context ctx) throws Exception {
        var rooms = roomRepository.findAll();
        var response = new java.util.ArrayList<>();

        for (var room : rooms) {
            var roomMap = new HashMap<String, Object>();
            roomMap.put("number", room.number);
            roomMap.put("type", room.type);
            roomMap.put("capacity", room.capacity);
            roomMap.put("nightlyRate", room.nightlyRate.doubleValue());
            response.add(roomMap);
        }

        ctx.json(response);
    }
}
