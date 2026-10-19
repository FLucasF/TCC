package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.ErrorResponse;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

public class ExceptionHandler {
    public static void register(Javalin app) {
        app.exception(InvalidRequestException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        });

        app.exception(ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        });

        app.exception(RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        });

        app.exception(OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current state"));
        });

        app.exception(CapacityExceededException.class, (e, ctx) -> {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
               .json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        });

        app.exception(NumberFormatException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid ID format"));
        });

        app.exception(com.fasterxml.jackson.databind.JsonMappingException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid request body"));
        });

        app.exception(com.fasterxml.jackson.core.JsonParseException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid JSON"));
        });
    }
}
