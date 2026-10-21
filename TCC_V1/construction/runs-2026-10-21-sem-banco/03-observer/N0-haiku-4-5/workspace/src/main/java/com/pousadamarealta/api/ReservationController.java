package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.CreateReservationRequest;
import com.pousadamarealta.api.dto.DocumentResponse;
import com.pousadamarealta.api.dto.ErrorResponse;
import com.pousadamarealta.api.dto.ReservationResponse;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.time.LocalDate;

public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

            if (!isValidDateFormat(req.checkIn()) || !isValidDateFormat(req.checkOut())) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Formato de data inválido"));
                return;
            }

            LocalDate checkIn = LocalDate.parse(req.checkIn());
            LocalDate checkOut = LocalDate.parse(req.checkOut());

            if (req.guestName() == null || req.guestEmail() == null || req.guestCpf() == null ||
                req.roomNumber() <= 0 || req.guests() < 1) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Campos obrigatórios faltando"));
                return;
            }

            var reservation = reservationService.createReservation(
                req.roomNumber(),
                req.guestName(),
                req.guestEmail(),
                req.guestCpf(),
                checkIn,
                checkOut,
                req.guests(),
                req.ratePlan()
            );

            ctx.status(201).json(ReservationResponse.from(reservation));
        } catch (ReservationService.ReservationException e) {
            handleReservationException(ctx, e);
        } catch (IllegalArgumentException | NullPointerException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Dados inválidos"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.getReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
        } catch (ReservationService.ReservationException e) {
            handleReservationException(ctx, e);
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
        } catch (ReservationService.ReservationException e) {
            handleReservationException(ctx, e);
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
        } catch (ReservationService.ReservationException e) {
            handleReservationException(ctx, e);
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
        } catch (ReservationService.ReservationException e) {
            handleReservationException(ctx, e);
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.checkOut(id);
            ctx.json(DocumentResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
        } catch (ReservationService.ReservationException e) {
            handleReservationException(ctx, e);
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.getReservation(id);

            if (reservation.status.name().equals("PENDING")) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Documento não disponível para reservas pendentes"));
                return;
            }

            ctx.json(DocumentResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "ID inválido"));
        } catch (ReservationService.ReservationException e) {
            handleReservationException(ctx, e);
        }
    }

    private void handleReservationException(Context ctx, ReservationService.ReservationException e) {
        int status = switch (e.errorCode) {
            case "ROOM_NOT_FOUND" -> 404;
            case "RESERVATION_NOT_FOUND" -> 404;
            case "ROOM_UNAVAILABLE" -> 409;
            case "OPERATION_NOT_ALLOWED" -> 409;
            case "CAPACITY_EXCEEDED" -> 422;
            default -> 400;
        };
        ctx.status(status).json(new ErrorResponse(e.errorCode, e.getMessage()));
    }

    private boolean isValidDateFormat(String date) {
        if (date == null || date.length() != 10) return false;
        try {
            LocalDate.parse(date);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
