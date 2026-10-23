package com.pousadamarealta.api.handler;

import com.pousadamarealta.api.dto.RoomResponse;
import com.pousadamarealta.api.mapper.ApiMapper;
import com.pousadamarealta.database.RoomDao;
import com.pousadamarealta.domain.Room;
import io.javalin.http.Context;

import java.util.List;

public class RoomHandler {
    private final RoomDao roomDao;

    public RoomHandler(RoomDao roomDao) {
        this.roomDao = roomDao;
    }

    public void getRooms(Context ctx) {
        List<Room> rooms = roomDao.findAll();
        List<RoomResponse> response = rooms.stream()
            .map(ApiMapper::toRoomResponse)
            .toList();
        ctx.json(response);
    }
}
