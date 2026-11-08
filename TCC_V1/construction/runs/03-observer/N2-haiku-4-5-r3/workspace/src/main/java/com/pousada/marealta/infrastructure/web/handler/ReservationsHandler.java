package com.pousada.marealta.infrastructure.web.handler;

import com.google.gson.Gson;
import com.pousada.marealta.application.reservation.CreateReservationRequest;
import com.pousada.marealta.application.reservation.ReservationService;
import com.pousada.marealta.domain.reservation.Reservation;
import com.pousada.marealta.domain.shared.Money;
import com.pousada.marealta.infrastructure.web.dto.ErrorResponse;
import com.pousada.marealta.infrastructure.web.dto.ReceiptDTO;
import com.pousada.marealta.infrastructure.web.dto.ReservationDTO;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationsHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReservationsHandler.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Gson gson = new Gson();

    private final ReservationService reservationService;

    public ReservationsHandler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        try {
            String body = ctx.body();
            CreateReservationRequest request = gson.fromJson(body, CreateReservationRequest.class);

            // Validate required fields
            if (request.roomNumber <= 0 || request.checkIn == null || request.checkOut == null ||
                request.guests <= 0 || request.guestName == null || request.guestEmail == null ||
                request.guestCpf == null || request.ratePlan == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            Reservation reservation = reservationService.createReservation(request);
            ctx.status(HttpStatus.CREATED);
            ctx.json(ReservationDTO.fromDomain(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating reservation", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.getReservationRepository().findById(id);

            if (reservation.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(ReservationDTO.fromDomain(reservation.get()));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            logger.error("Error getting reservation", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            String body = ctx.body();

            // Parse amount from body { "amount": 123.45 }
            var json = gson.fromJson(body, java.util.Map.class);
            double amount = (double) json.getOrDefault("amount", 0.0);

            if (amount <= 0) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Amount must be positive"));
                return;
            }

            Money amountPaid = Money.of(amount);
            Reservation reservation = reservationService.confirmReservation(id, amountPaid);

            ctx.json(ReservationDTO.fromDomain(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error confirming reservation", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationDTO.fromDomain(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error checking in", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            ctx.json(ReceiptDTO.fromDomain(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error checking out", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationDTO.fromDomain(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error cancelling reservation", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.getReservationRepository().findById(id);

            if (reservation.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(ReceiptDTO.fromDomain(reservation.get()));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            logger.error("Error getting document", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    private void handleError(Context ctx, String message) {
        if (message.contains("Room not found")) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("ROOM_NOT_FOUND", message));
        } else if (message.contains("Reservation not found")) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", message));
        } else if (message.contains("already booked")) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", message));
        } else if (message.contains("exceeds room capacity")) {
            ctx.status(422);
            ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", message));
        } else {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", message));
        }
    }
}
