package com.pousadamarealta.controller;

import com.pousadamarealta.domain.enums.RatePlan;
import com.pousadamarealta.domain.model.Document;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.dto.CreateReservationRequest;
import com.pousadamarealta.dto.DocumentResponse;
import com.pousadamarealta.dto.ErrorResponse;
import com.pousadamarealta.dto.ReservationResponse;
import com.pousadamarealta.service.DocumentService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final ReservationService reservationService;
    private final DocumentService documentService;

    public ReservationController(ReservationService reservationService, DocumentService documentService) {
        this.reservationService = reservationService;
        this.documentService = documentService;
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            validateRequest(request);

            LocalDate checkIn = LocalDate.parse(request.checkIn());
            LocalDate checkOut = LocalDate.parse(request.checkOut());
            RatePlan ratePlan = RatePlan.valueOf(request.ratePlan());

            Reservation reservation = reservationService.createReservation(
                request.roomNumber(),
                request.guestName(),
                request.guestEmail(),
                request.guestCpf(),
                checkIn,
                checkOut,
                request.guests(),
                ratePlan
            );

            ctx.status(201).json(ReservationResponse.from(reservation));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id)
                    .orElseThrow(() -> new ReservationService.ReservationNotFoundException(id));
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException | IllegalStateException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException | IllegalStateException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException | IllegalStateException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            Document document = documentService.generateDocument(reservation);
            ctx.json(DocumentResponse.from(document));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException | IllegalStateException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id)
                    .orElseThrow(() -> new ReservationService.ReservationNotFoundException(id));

            if (!reservation.isPaid()) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED",
                    "Document only available for paid reservations"));
                return;
            }

            Document document = documentService.generateDocument(reservation);
            ctx.json(DocumentResponse.from(document));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.roomNumber().isBlank()) {
            throw new ReservationService.InvalidRequestException("Room number is required");
        }
        if (request.guestName() == null || request.guestName().isBlank()) {
            throw new ReservationService.InvalidRequestException("Guest name is required");
        }
        if (request.guestEmail() == null || request.guestEmail().isBlank()) {
            throw new ReservationService.InvalidRequestException("Guest email is required");
        }
        if (request.guestCpf() == null || request.guestCpf().isBlank()) {
            throw new ReservationService.InvalidRequestException("Guest CPF is required");
        }
        if (request.checkIn() == null || request.checkIn().isBlank()) {
            throw new ReservationService.InvalidRequestException("Check-in date is required");
        }
        if (request.checkOut() == null || request.checkOut().isBlank()) {
            throw new ReservationService.InvalidRequestException("Check-out date is required");
        }
        if (request.ratePlan() == null || request.ratePlan().isBlank()) {
            throw new ReservationService.InvalidRequestException("Rate plan is required");
        }
    }
}
