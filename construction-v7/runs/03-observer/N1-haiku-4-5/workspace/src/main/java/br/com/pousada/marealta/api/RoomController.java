package br.com.pousada.marealta.api;

import br.com.pousada.marealta.api.dto.RoomResponse;
import br.com.pousada.marealta.service.ReservationService;
import io.javalin.Javalin;

public class RoomController {
    public static void register(Javalin app, ReservationService reservationService) {
        app.get("/rooms", ctx -> {
            var rooms = reservationService.getRooms()
                    .stream()
                    .map(RoomResponse::from)
                    .toList();
            ctx.json(rooms);
        });
    }
}
