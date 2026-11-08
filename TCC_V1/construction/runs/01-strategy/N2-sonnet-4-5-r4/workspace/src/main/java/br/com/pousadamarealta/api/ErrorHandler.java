package br.com.pousadamarealta.api;

import br.com.pousadamarealta.api.dto.ErrorResponse;
import br.com.pousadamarealta.domain.exception.*;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

public class ErrorHandler {

    public static void configure(Javalin app) {
        app.exception(InvalidRequestException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        });

        app.exception(RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        });

        app.exception(ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        });

        app.exception(RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        });

        app.exception(OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        });

        app.exception(CapacityExceededException.class, (e, ctx) -> {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
               .json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        });

        app.exception(NumberFormatException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
        });

        app.exception(com.fasterxml.jackson.databind.JsonMappingException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "JSON inválido ou campos faltando"));
        });

        app.exception(com.fasterxml.jackson.core.JsonProcessingException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Corpo da requisição não é um JSON válido"));
        });
    }
}
