package com.pousadamarealta.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pousadamarealta.api.dto.ErrorResponse;
import com.pousadamarealta.exception.*;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class ExceptionHandler {

    public static void handleInvalidRequest(InvalidRequestException e, Context ctx) {
        ctx.status(HttpStatus.BAD_REQUEST)
           .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
    }

    public static void handleRoomNotFound(RoomNotFoundException e, Context ctx) {
        ctx.status(HttpStatus.NOT_FOUND)
           .json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
    }

    public static void handleReservationNotFound(ReservationNotFoundException e, Context ctx) {
        ctx.status(HttpStatus.NOT_FOUND)
           .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
    }

    public static void handleRoomUnavailable(RoomUnavailableException e, Context ctx) {
        ctx.status(HttpStatus.CONFLICT)
           .json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
    }

    public static void handleOperationNotAllowed(OperationNotAllowedException e, Context ctx) {
        ctx.status(HttpStatus.CONFLICT)
           .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
    }

    public static void handleCapacityExceeded(CapacityExceededException e, Context ctx) {
        ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
           .json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
    }

    public static void handleJsonProcessing(JsonProcessingException e, Context ctx) {
        ctx.status(HttpStatus.BAD_REQUEST)
           .json(new ErrorResponse("INVALID_REQUEST", "Invalid JSON format"));
    }

    public static void handleGenericException(Exception e, Context ctx) {
        e.printStackTrace();
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
           .json(new ErrorResponse("INTERNAL_ERROR", "An internal error occurred"));
    }
}
