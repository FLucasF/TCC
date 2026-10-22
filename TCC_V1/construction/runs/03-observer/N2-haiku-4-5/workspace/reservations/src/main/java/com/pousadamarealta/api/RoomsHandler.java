package com.pousadamarealta.api;

import com.pousadamarealta.database.RoomDAO;
import com.pousadamarealta.domain.Room;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoomsHandler {
    public static void getRooms(Context ctx) throws Exception {
        List<Room> rooms = RoomDAO.getInstance().getAllRooms();
        List<Map<String, Object>> response = new ArrayList<>();

        for (Room room : rooms) {
            response.add(Map.of(
                    "number", room.getNumber(),
                    "type", room.getTypeCode(),
                    "capacity", room.getCapacity(),
                    "nightlyRate", room.getNightlyRate()
            ));
        }

        ctx.json(response);
    }
}
