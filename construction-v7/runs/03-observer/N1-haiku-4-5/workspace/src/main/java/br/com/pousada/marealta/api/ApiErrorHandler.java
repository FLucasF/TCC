package br.com.pousada.marealta.api;

import br.com.pousada.marealta.api.dto.ErrorResponse;
import io.javalin.Javalin;

public class ApiErrorHandler {
    public static void register(Javalin app) {
        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            String errorCode = e.getMessage();

            int status;
            String message;

            switch (errorCode) {
                case "INVALID_REQUEST":
                    status = 400;
                    message = "Requisição inválida";
                    break;
                case "ROOM_NOT_FOUND":
                    status = 404;
                    message = "Quarto não encontrado";
                    break;
                case "RESERVATION_NOT_FOUND":
                    status = 404;
                    message = "Reserva não encontrada";
                    break;
                case "ROOM_UNAVAILABLE":
                    status = 409;
                    message = "Quarto indisponível para as datas solicitadas";
                    break;
                case "CAPACITY_EXCEEDED":
                    status = 422;
                    message = "Número de hóspedes exceeds room capacity";
                    break;
                case "OPERATION_NOT_ALLOWED":
                    status = 409;
                    message = "Operação não permitida para o estado atual da reserva";
                    break;
                default:
                    status = 400;
                    message = "Erro ao processar requisição";
                    errorCode = "INVALID_REQUEST";
            }

            ctx.status(status);
            ctx.json(new ErrorResponse(errorCode, message));
        });
    }
}
