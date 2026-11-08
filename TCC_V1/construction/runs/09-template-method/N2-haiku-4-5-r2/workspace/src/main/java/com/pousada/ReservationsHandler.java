package com.pousada;

import com.google.gson.Gson;
import com.pousada.api.dto.CreateReservationRequest;
import com.pousada.api.dto.ReceiptResponse;
import com.pousada.api.dto.ReservationResponse;
import com.pousada.application.ApiException;
import com.pousada.application.ReservationService;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Rounding;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReservationsHandler {
    private final ReservationService reservationService = new ReservationService();
    private final Gson gson = new Gson();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    public void create(Context ctx) {
        try {
            CreateReservationRequest request = gson.fromJson(ctx.body(), CreateReservationRequest.class);

            if (request == null || request.roomNumber == 0 || request.guestName == null ||
                request.guestEmail == null || request.guestCpf == null || request.checkIn == null ||
                request.checkOut == null || request.guests == 0 || request.ratePlan == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            if (request.guestName.isEmpty() || request.guestEmail.isEmpty() || request.guestCpf.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Fields cannot be empty"));
                return;
            }

            if (!request.guestCpf.matches("\\d{11}")) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "CPF must contain exactly 11 digits"));
                return;
            }

            LocalDate checkIn = parseDate(request.checkIn);
            LocalDate checkOut = parseDate(request.checkOut);
            RatePlan ratePlan = RatePlan.valueOf(request.ratePlan);

            var reservation = reservationService.createReservation(
                request.roomNumber, request.guestName, request.guestEmail,
                request.guestCpf, checkIn, checkOut, request.guests, ratePlan
            );

            ctx.status(201);
            ctx.json(ReservationResponse.fromDomain(reservation));
        } catch (ApiException e) {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid enum value or date format"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void get(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.getReservation(id);
            ctx.json(ReservationResponse.fromDomain(reservation));
        } catch (ApiException e) {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void confirm(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.getReservation(id);
            double amountToPay = Rounding.round(reservation.getTotal());
            var confirmedReservation = reservationService.confirmReservation(id, amountToPay);
            ctx.json(ReservationResponse.fromDomain(confirmedReservation));
        } catch (ApiException e) {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.checkInReservation(id);
            ctx.json(ReservationResponse.fromDomain(reservation));
        } catch (ApiException e) {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.checkOutReservation(id);
            String receipt = reservationService.getReceiptDocument(id);
            ctx.result(receipt);
        } catch (ApiException e) {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void cancel(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.fromDomain(reservation));
        } catch (ApiException e) {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            String receipt = reservationService.getReceiptDocument(id);
            ctx.result(receipt);
        } catch (ApiException e) {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private LocalDate parseDate(String dateString) {
        try {
            return LocalDate.parse(dateString, dateFormatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format");
        }
    }

    static class ErrorResponse {
        String error;
        String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
