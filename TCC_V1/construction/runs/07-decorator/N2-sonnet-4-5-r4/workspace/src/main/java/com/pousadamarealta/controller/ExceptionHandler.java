package com.pousadamarealta.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pousadamarealta.dto.ErrorDTO;
import com.pousadamarealta.service.*;
import io.javalin.http.Context;
import io.javalin.validation.ValidationException;

import java.time.format.DateTimeParseException;

public class ExceptionHandler {

    public static void register(io.javalin.Javalin app) {
        app.exception(ValidationException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", "Invalid request parameters"));
        });

        app.exception(JsonProcessingException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", "Invalid JSON format"));
        });

        app.exception(DateTimeParseException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", "Invalid date format"));
        });

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorDTO("ROOM_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorDTO("RESERVATION_NOT_FOUND", e.getMessage()));
        });

        app.exception(RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorDTO("ROOM_UNAVAILABLE", e.getMessage()));
        });

        app.exception(OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorDTO("OPERATION_NOT_ALLOWED", e.getMessage()));
        });

        app.exception(CapacityExceededException.class, (e, ctx) -> {
            ctx.status(422).json(new ErrorDTO("CAPACITY_EXCEEDED", e.getMessage()));
        });

        app.exception(IllegalStateException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorDTO("OPERATION_NOT_ALLOWED", e.getMessage()));
        });
    }
}
