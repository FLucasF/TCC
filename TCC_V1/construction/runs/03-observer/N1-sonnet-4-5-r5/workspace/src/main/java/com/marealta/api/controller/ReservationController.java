package com.marealta.api.controller;

import com.marealta.api.dto.CreateReservationRequest;
import com.marealta.api.dto.DocumentResponse;
import com.marealta.api.dto.ReservationResponse;
import com.marealta.api.error.ErrorHandler;
import com.marealta.domain.model.Reservation;
import com.marealta.domain.repository.ReservationRepository;
import com.marealta.domain.repository.RoomRepository;
import com.marealta.domain.service.ReservationService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;

    public ReservationController(ReservationService reservationService, ReservationRepository reservationRepository) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
    }

    public void create(Context ctx) {
        CreateReservationRequest request;
        try {
            request = ctx.bodyAsClass(CreateReservationRequest.class);
        } catch (Exception e) {
            throw new ErrorHandler.InvalidRequestException("Invalid request body");
        }

        validateRequest(request);

        Reservation reservation = reservationService.createReservation(
            request.roomNumber(),
            request.guestName(),
            request.guestEmail(),
            request.guestCpf(),
            request.checkIn(),
            request.checkOut(),
            request.guests(),
            request.ratePlan()
        );

        ctx.status(HttpStatus.CREATED).json(ReservationResponse.from(reservation));
    }

    public void get(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationService.ReservationNotFoundException(id));
        ctx.json(ReservationResponse.from(reservation));
    }

    public void confirm(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void cancel(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkOut(id);
        String roomType = getRoomType(reservation.getRoomNumber());
        ctx.json(DocumentResponse.from(reservation, roomType));
    }

    public void getDocument(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationService.ReservationNotFoundException(id));

        if (!reservation.isPaid()) {
            throw new ReservationService.OperationNotAllowedException(
                "Document is only available for paid reservations"
            );
        }

        String roomType = getRoomType(reservation.getRoomNumber());
        ctx.json(DocumentResponse.from(reservation, roomType));
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.roomNumber().isBlank()) {
            throw new ErrorHandler.InvalidRequestException("Room number is required");
        }
        if (request.guestName() == null || request.guestName().isBlank()) {
            throw new ErrorHandler.InvalidRequestException("Guest name is required");
        }
        if (request.guestEmail() == null || request.guestEmail().isBlank()) {
            throw new ErrorHandler.InvalidRequestException("Guest email is required");
        }
        if (request.guestCpf() == null || request.guestCpf().isBlank()) {
            throw new ErrorHandler.InvalidRequestException("Guest CPF is required");
        }
        if (request.checkIn() == null) {
            throw new ErrorHandler.InvalidRequestException("Check-in date is required");
        }
        if (request.checkOut() == null) {
            throw new ErrorHandler.InvalidRequestException("Check-out date is required");
        }
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new ErrorHandler.InvalidRequestException("Check-out must be after check-in");
        }
        if (request.ratePlan() == null) {
            throw new ErrorHandler.InvalidRequestException("Rate plan is required");
        }
    }

    private String getRoomType(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }
}
