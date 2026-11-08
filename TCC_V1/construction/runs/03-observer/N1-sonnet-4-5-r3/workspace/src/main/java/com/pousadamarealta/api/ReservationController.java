package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.CreateReservationRequest;
import com.pousadamarealta.domain.DocumentFormatter;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;

    public ReservationController(ReservationService reservationService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        if (request.roomNumber() == null || request.guestName() == null || request.guestEmail() == null ||
            request.guestCpf() == null || request.checkIn() == null || request.checkOut() == null ||
            request.guests() == null || request.ratePlan() == null) {
            throw new ReservationService.InvalidRequestException("Missing required fields");
        }

        LocalDate checkIn = parseDate(request.checkIn());
        LocalDate checkOut = parseDate(request.checkOut());

        Reservation reservation = reservationService.createReservation(
            request.roomNumber(),
            request.guestName(),
            request.guestEmail(),
            request.guestCpf(),
            checkIn,
            checkOut,
            request.guests(),
            request.ratePlan()
        );

        ctx.status(201).json(mapReservation(reservation));
    }

    public void getReservation(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(mapReservation(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(mapReservation(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(mapReservation(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        ctx.json(mapDocument(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(mapReservation(reservation));
    }

    public void getDocument(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);

        if (!reservation.isPaid()) {
            throw new ReservationService.OperationNotAllowedException("Document only available for paid reservations");
        }

        ctx.json(mapDocument(reservation));
    }

    private Map<String, Object> mapReservation(Reservation reservation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", reservation.getCheckIn().toString());
        map.put("checkOut", reservation.getCheckOut().toString());
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan().toString());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        map.put("status", reservation.getStatus().toString());
        map.put("amountPaid", reservation.getAmountPaid());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private Map<String, Object> mapDocument(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", reservation.getId());
        map.put("guestName", reservation.getGuestName());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("roomType", room.getType().toString());
        map.put("checkIn", reservation.getCheckIn().toString());
        map.put("checkOut", reservation.getCheckOut().toString());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        return map;
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ReservationService.InvalidRequestException("Invalid date format: " + date);
        }
    }

    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new ReservationService.InvalidRequestException("Invalid id format: " + id);
        }
    }
}
