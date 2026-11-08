package br.com.pousadamarealta.api;

import br.com.pousadamarealta.api.dto.RoomDTO;
import br.com.pousadamarealta.repository.RoomRepository;
import io.javalin.Javalin;

import java.util.List;

public class RoomsController {
    private final RoomRepository roomRepository;

    public RoomsController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void register(Javalin app) {
        app.get("/rooms", ctx -> {
            List<RoomDTO> rooms = roomRepository.findAll().stream()
                .map(RoomDTO::from)
                .toList();
            ctx.json(rooms);
        });
    }
}
