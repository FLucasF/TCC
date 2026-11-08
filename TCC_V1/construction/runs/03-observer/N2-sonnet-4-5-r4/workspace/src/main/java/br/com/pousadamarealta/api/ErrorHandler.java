package br.com.pousadamarealta.api;

import br.com.pousadamarealta.api.dto.ErrorResponse;
import br.com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

public class ErrorHandler {

    public static void register(Javalin app) {
        app.exception(ReservationService.RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        });

        app.exception(ReservationService.ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        });

        app.exception(ReservationService.InvalidRequestException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(ReservationService.CapacityExceededException.class, (e, ctx) -> {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        });

        app.exception(ReservationService.RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        });

        app.exception(ReservationService.OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current reservation status"));
        });
    }
}
