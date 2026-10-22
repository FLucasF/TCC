package br.com.pousadamarealta.api;

import br.com.pousadamarealta.api.dto.RoomDTO;
import br.com.pousadamarealta.service.RoomService;
import io.javalin.http.Context;

import java.util.List;

public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    public void getRooms(Context ctx) {
        List<RoomDTO> rooms = roomService.getAllRooms().stream()
            .map(RoomDTO::from)
            .toList();
        ctx.json(rooms);
    }
}
