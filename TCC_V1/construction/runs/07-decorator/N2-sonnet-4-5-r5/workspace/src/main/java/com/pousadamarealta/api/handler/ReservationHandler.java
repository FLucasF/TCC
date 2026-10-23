package com.pousadamarealta.api.handler;

import com.pousadamarealta.api.dto.CreateReservationRequest;
import com.pousadamarealta.api.dto.DocumentResponse;
import com.pousadamarealta.api.dto.ErrorResponse;
import com.pousadamarealta.api.dto.ReservationResponse;
import com.pousadamarealta.api.mapper.ApiMapper;
import com.pousadamarealta.database.RoomDao;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.exceptions.*;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationHandler {
    private final ReservationService reservationService;
    private final RoomDao roomDao;

    public ReservationHandler(ReservationService reservationService, RoomDao roomDao) {
        this.reservationService = reservationService;
        this.roomDao = roomDao;
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

            ctx.status(201).json(ApiMapper.toReservationResponse(reservation));

        } catch (InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } catch (RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        } catch (CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(ApiMapper.toReservationResponse(reservation));

        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ApiMapper.toReservationResponse(reservation));

        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ApiMapper.toReservationResponse(reservation));

        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            DocumentResponse document = ApiMapper.toDocumentResponse(reservation, roomDao);
            ctx.json(document);

        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ApiMapper.toReservationResponse(reservation));

        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getStatus() == ReservationStatus.PENDING) {
                throw new OperationNotAllowedException("Document only available for paid reservations");
            }

            DocumentResponse document = ApiMapper.toDocumentResponse(reservation, roomDao);
            ctx.json(document);

        } catch (ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.roomNumber().isBlank()) {
            throw new InvalidRequestException("Room number is required");
        }
        if (request.guestName() == null || request.guestName().isBlank()) {
            throw new InvalidRequestException("Guest name is required");
        }
        if (request.guestEmail() == null || request.guestEmail().isBlank()) {
            throw new InvalidRequestException("Guest email is required");
        }
        if (request.guestCpf() == null || request.guestCpf().isBlank()) {
            throw new InvalidRequestException("Guest CPF is required");
        }
        if (request.checkIn() == null || request.checkIn().isBlank()) {
            throw new InvalidRequestException("Check-in date is required");
        }
        if (request.checkOut() == null || request.checkOut().isBlank()) {
            throw new InvalidRequestException("Check-out date is required");
        }
        if (request.guests() == null) {
            throw new InvalidRequestException("Number of guests is required");
        }
        if (request.ratePlan() == null || request.ratePlan().isBlank()) {
            throw new InvalidRequestException("Rate plan is required");
        }

        try {
            LocalDate.parse(request.checkIn());
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid check-in date format");
        }

        try {
            LocalDate.parse(request.checkOut());
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid check-out date format");
        }

        try {
            RatePlan.valueOf(request.ratePlan());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid rate plan");
        }
    }
}
