package com.marealta.api.error;

import com.marealta.domain.service.ReservationService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class ErrorHandler {

    public static void handle(Exception e, Context ctx) {
        if (e instanceof ReservationService.RoomNotFoundException) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } else if (e instanceof ReservationService.ReservationNotFoundException) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } else if (e instanceof ReservationService.RoomUnavailableException) {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        } else if (e instanceof ReservationService.OperationNotAllowedException) {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } else if (e instanceof ReservationService.CapacityExceededException) {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
               .json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        } else if (e instanceof ReservationService.InvalidRequestException) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } else if (e instanceof InvalidRequestException) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } else {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}
