package com.pousada.marealta.handler;

import com.pousada.marealta.model.Room;
import com.pousada.marealta.model.RoomDto;
import com.pousada.marealta.persistence.RoomDao;
import io.javalin.http.Context;

import java.util.List;

public class RoomHandler {
    private final RoomDao roomDao;

    public RoomHandler(RoomDao roomDao) {
        this.roomDao = roomDao;
    }

    public void listRooms(Context ctx) {
        try {
            List<Room> rooms = roomDao.findAll();
            ctx.json(rooms.stream().map(room -> new RoomDto(
                room.getNumber(),
                room.getType().name(),
                room.getCapacity(),
                room.getNightlyRate()
            )).toList());
        } catch (Exception e) {
            ErrorHandler.handleException(ctx, e);
        }
    }
}
