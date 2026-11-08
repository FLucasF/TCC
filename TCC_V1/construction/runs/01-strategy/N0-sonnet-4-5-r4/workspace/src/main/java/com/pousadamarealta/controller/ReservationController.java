package com.pousadamarealta.controller;

import com.pousadamarealta.dto.CreateReservationRequest;
import com.pousadamarealta.dto.DocumentResponse;
import com.pousadamarealta.dto.ReservationResponse;
import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService;
    private final RoomRepository roomRepository;

    public ReservationController(ReservationService reservationService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(request.roomNumber());
        reservation.setGuestName(request.guestName());
        reservation.setGuestEmail(request.guestEmail());
        reservation.setGuestCpf(request.guestCpf());
        reservation.setCheckIn(parseDate(request.checkIn()));
        reservation.setCheckOut(parseDate(request.checkOut()));
        reservation.setGuests(request.guests());
        reservation.setRatePlan(parseRatePlan(request.ratePlan()));
        reservation.setCompanyCode(request.companyCode());

        Reservation created = reservationService.createReservation(reservation);

        ctx.status(201).json(toResponse(created));
    }

    public void getReservation(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(toResponse(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(toResponse(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(toResponse(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toResponse(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(toDocumentResponse(reservation, room.getType().name()));
    }

    public void getDocument(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);

        if (reservation.getAmountPaid().compareTo(java.math.BigDecimal.ZERO) == 0) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Document is only available for paid reservations");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(toDocumentResponse(reservation, room.getType().name()));
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.guestName() == null ||
            request.guestEmail() == null || request.guestCpf() == null ||
            request.checkIn() == null || request.checkOut() == null ||
            request.ratePlan() == null) {
            throw new ApiException(400, "INVALID_REQUEST", "Missing required fields");
        }
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid date format. Expected: yyyy-MM-dd");
        }
    }

    private RatePlan parseRatePlan(String ratePlan) {
        try {
            return RatePlan.valueOf(ratePlan);
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid rate plan: " + ratePlan);
        }
    }

    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid reservation ID");
        }
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn().format(DATE_FORMAT),
            reservation.getCheckOut().format(DATE_FORMAT),
            reservation.getGuests(),
            reservation.getRatePlan().name(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            reservation.getStatus().name(),
            reservation.getAmountPaid(),
            reservation.getRefund()
        );
    }

    private DocumentResponse toDocumentResponse(Reservation reservation, String roomType) {
        return new DocumentResponse(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            roomType,
            reservation.getCheckIn().format(DATE_FORMAT),
            reservation.getCheckOut().format(DATE_FORMAT),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }
}
