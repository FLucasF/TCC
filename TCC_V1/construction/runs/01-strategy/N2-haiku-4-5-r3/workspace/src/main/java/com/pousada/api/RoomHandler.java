package com.pousada.api;

import com.pousada.database.RoomDao;
import com.pousada.model.Room;
import io.javalin.http.Context;

import java.util.List;

public class RoomHandler {
    public static void listRooms(Context ctx) throws Exception {
        List<Room> rooms = RoomDao.findAll();
        ctx.json(rooms.stream().map(r -> new RoomDto(r.number, r.type, r.capacity, r.nightlyRate)).toList());
    }

    static class RoomDto {
        public int number;
        public String type;
        public int capacity;
        public Object nightlyRate;

        RoomDto(int number, String type, int capacity, Object nightlyRate) {
            this.number = number;
            this.type = type;
            this.capacity = capacity;
            this.nightlyRate = nightlyRate;
        }
    }
}
