package com.pousadamarealta.controller;

import com.pousadamarealta.dto.CreateReservationRequest;
import com.pousadamarealta.dto.DocumentDTO;
import com.pousadamarealta.dto.ReservationDTO;
import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.mapper.ModelMapper;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomService roomService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public ReservationController(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        LocalDate checkIn = parseDate(request.getCheckIn());
        LocalDate checkOut = parseDate(request.getCheckOut());

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out date must be after check-in date");
        }

        if (request.getGuests() < 1) {
            throw new InvalidRequestException("Number of guests must be at least 1");
        }

        RatePlan ratePlan;
        try {
            ratePlan = RatePlan.valueOf(request.getRatePlan());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid rate plan: " + request.getRatePlan());
        }

        Reservation reservation = reservationService.createReservation(
            request.getRoomNumber(),
            request.getGuestName(),
            request.getGuestEmail(),
            request.getGuestCpf(),
            checkIn,
            checkOut,
            request.getGuests(),
            ratePlan
        );

        ctx.status(201).json(ModelMapper.toDTO(reservation));
    }

    public void getReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.findById(id);
        ctx.json(ModelMapper.toDTO(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ModelMapper.toDTO(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ModelMapper.toDTO(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ModelMapper.toDTO(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkOut(id);
        String roomType = roomService.findByNumber(reservation.getRoomNumber()).get().getType().name();
        DocumentDTO document = ModelMapper.toDocumentDTO(reservation, roomType);
        ctx.json(document);
    }

    public void getDocument(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.findById(id);
        String roomType = roomService.findByNumber(reservation.getRoomNumber()).get().getType().name();
        DocumentDTO document = ModelMapper.toDocumentDTO(reservation, roomType);
        ctx.json(document);
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.getRoomNumber() == null || request.getRoomNumber().isBlank()) {
            throw new InvalidRequestException("Room number is required");
        }
        if (request.getGuestName() == null || request.getGuestName().isBlank()) {
            throw new InvalidRequestException("Guest name is required");
        }
        if (request.getGuestEmail() == null || request.getGuestEmail().isBlank()) {
            throw new InvalidRequestException("Guest email is required");
        }
        if (request.getGuestCpf() == null || request.getGuestCpf().isBlank()) {
            throw new InvalidRequestException("Guest CPF is required");
        }
        if (request.getCheckIn() == null || request.getCheckIn().isBlank()) {
            throw new InvalidRequestException("Check-in date is required");
        }
        if (request.getCheckOut() == null || request.getCheckOut().isBlank()) {
            throw new InvalidRequestException("Check-out date is required");
        }
        if (request.getRatePlan() == null || request.getRatePlan().isBlank()) {
            throw new InvalidRequestException("Rate plan is required");
        }
    }

    private LocalDate parseDate(String dateString) {
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format. Expected: yyyy-MM-dd");
        }
    }
}
