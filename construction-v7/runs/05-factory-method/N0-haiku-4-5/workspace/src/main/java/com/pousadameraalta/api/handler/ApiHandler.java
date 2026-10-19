package com.pousadameraalta.api.handler;

import com.google.gson.Gson;
import com.pousadameraalta.api.dto.*;
import com.pousadameraalta.api.validation.RequestValidator;
import com.pousadameraalta.model.RatePlan;
import com.pousadameraalta.model.Reservation;
import com.pousadameraalta.repository.DataStore;
import com.pousadameraalta.service.ReservationService;
import com.pousadameraalta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.util.stream.Collectors;

public class ApiHandler {
    private final Gson gson = new Gson();
    private final ReservationService reservationService = new ReservationService();
    private final ReportService reportService = new ReportService();
    private final DataStore dataStore = DataStore.getInstance();

    public void getRooms(Context ctx) {
        var rooms = dataStore.getAllRooms().stream()
                .map(RoomResponse::new)
                .collect(Collectors.toList());
        ctx.json(rooms);
    }

    public void createReservation(Context ctx) {
        try {
            ReservationRequest req = gson.fromJson(ctx.body(), ReservationRequest.class);

            RequestValidator.validateCreateReservation(req);

            LocalDate checkIn = LocalDate.parse(req.checkIn);
            LocalDate checkOut = LocalDate.parse(req.checkOut);

            Reservation reservation = reservationService.createReservation(
                    req.roomNumber,
                    req.guestName,
                    req.guestEmail,
                    req.guestCpf,
                    req.companyCnpj,
                    req.companyName,
                    req.guestPassport,
                    req.guestCountry,
                    checkIn,
                    checkOut,
                    req.guests,
                    RatePlan.valueOf(req.ratePlan)
            );

            ctx.status(201);
            ctx.json(ReservationResponse.fromModel(reservation, req.checkIn, req.checkOut));
        } catch (RequestValidator.ValidationException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse(e.errorCode, e.errorMessage));
        } catch (Exception e) {
            handleServiceException(ctx, e);
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(ReservationResponse.fromModel(
                    reservation,
                    reservation.getCheckIn().toString(),
                    reservation.getCheckOut().toString()
            ));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            handleServiceException(ctx, e);
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationResponse.fromModel(
                    reservation,
                    reservation.getCheckIn().toString(),
                    reservation.getCheckOut().toString()
            ));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            handleServiceException(ctx, e);
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.fromModel(
                    reservation,
                    reservation.getCheckIn().toString(),
                    reservation.getCheckOut().toString()
            ));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            handleServiceException(ctx, e);
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.fromModel(
                    reservation,
                    reservation.getCheckIn().toString(),
                    reservation.getCheckOut().toString()
            ));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            handleServiceException(ctx, e);
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            reservationService.checkOut(id);
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(DocumentResponse.fromReservation(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            handleServiceException(ctx, e);
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getAmountPaid().signum() == 0) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Document not available for unpaid reservations"));
                return;
            }

            ctx.json(DocumentResponse.fromReservation(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            handleServiceException(ctx, e);
        }
    }

    public void getMessages(Context ctx) {
        var messages = dataStore.getAllMessages().stream()
                .map(MessageResponse::new)
                .collect(Collectors.toList());
        ctx.json(messages);
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String date = ctx.queryParam("date");
            if (date == null || date.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
                return;
            }

            RequestValidator.validateDate(date);
            LocalDate localDate = LocalDate.parse(date);

            String csv = reportService.generateOccupancyReport(localDate);
            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(csv);
        } catch (RequestValidator.ValidationException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse(e.errorCode, e.errorMessage));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String month = ctx.queryParam("month");
            if (month == null || month.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
                return;
            }

            RequestValidator.validateMonth(month);
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(csv);
        } catch (RequestValidator.ValidationException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse(e.errorCode, e.errorMessage));
        } catch (Exception e) {
            handleServiceException(ctx, e);
        }
    }

    private void handleServiceException(Context ctx, Exception e) {
        String message = e.getMessage();

        if ("ROOM_NOT_FOUND".equals(message)) {
            ctx.status(404);
            ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } else if ("RESERVATION_NOT_FOUND".equals(message)) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } else if ("ROOM_UNAVAILABLE".equals(message)) {
            ctx.status(409);
            ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for these dates"));
        } else if ("CAPACITY_EXCEEDED".equals(message)) {
            ctx.status(422);
            ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Guests exceed room capacity"));
        } else if ("OPERATION_NOT_ALLOWED".equals(message)) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for current reservation status"));
        } else if ("INVALID_REQUEST".equals(message)) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        } else {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "An internal error occurred"));
        }
    }
}
