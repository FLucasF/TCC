package com.pousada.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pousada.api.dto.ErrorResponse;
import com.pousada.domain.validation.ValidationException;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class ErrorHandler {

    public static void handleValidationException(ValidationException e, Context ctx) {
        HttpStatus status = switch (e.getErrorCode()) {
            case "ROOM_NOT_FOUND", "RESERVATION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "ROOM_UNAVAILABLE", "OPERATION_NOT_ALLOWED" -> HttpStatus.CONFLICT;
            case "CAPACITY_EXCEEDED" -> HttpStatus.UNPROCESSABLE_CONTENT;
            default -> HttpStatus.BAD_REQUEST;
        };

        ctx.status(status);
        ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    public static void handleGenericException(Exception e, Context ctx) {
        if (e instanceof JsonProcessingException) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid JSON format"));
        } else {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }
}
