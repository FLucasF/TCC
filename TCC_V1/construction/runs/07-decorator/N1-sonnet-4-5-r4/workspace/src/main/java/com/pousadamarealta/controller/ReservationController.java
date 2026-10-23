package com.pousadamarealta.controller;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationDocument;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;

    public ReservationController(ReservationService reservationService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    public void create(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            Reservation reservation = new Reservation();
            reservation.setRoomNumber((String) body.get("roomNumber"));
            reservation.setGuestName((String) body.get("guestName"));
            reservation.setGuestEmail((String) body.get("guestEmail"));
            reservation.setGuestCpf((String) body.get("guestCpf"));

            try {
                reservation.setCheckIn(LocalDate.parse((String) body.get("checkIn")));
                reservation.setCheckOut(LocalDate.parse((String) body.get("checkOut")));
            } catch (DateTimeParseException e) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Invalid date format"
                ));
                return;
            }

            if (reservation.getCheckOut().isBefore(reservation.getCheckIn()) ||
                reservation.getCheckOut().isEqual(reservation.getCheckIn())) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Check-out must be after check-in"
                ));
                return;
            }

            int guests = ((Number) body.get("guests")).intValue();
            if (guests < 1) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "At least one guest is required"
                ));
                return;
            }
            reservation.setGuests(guests);

            try {
                reservation.setRatePlan(RatePlan.valueOf((String) body.get("ratePlan")));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Invalid rate plan"
                ));
                return;
            }

            Reservation created = reservationService.create(reservation);
            ctx.status(201).json(toMap(created));

        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(Map.of(
                "error", "ROOM_NOT_FOUND",
                "message", "Room not found"
            ));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(Map.of(
                "error", "CAPACITY_EXCEEDED",
                "message", "Number of guests exceeds room capacity"
            ));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(Map.of(
                "error", "ROOM_UNAVAILABLE",
                "message", "Room is not available for the selected dates"
            ));
        } catch (Exception e) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", e.getMessage()
            ));
        }
    }

    public void getById(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.findById(id);
            ctx.json(toMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        }
    }

    public void confirm(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirm(id);
            ctx.json(toMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(Map.of(
                "error", "OPERATION_NOT_ALLOWED",
                "message", "Operation not allowed for current reservation status"
            ));
        }
    }

    public void cancel(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancel(id);
            ctx.json(toMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(Map.of(
                "error", "OPERATION_NOT_ALLOWED",
                "message", "Operation not allowed for current reservation status"
            ));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(Map.of(
                "error", "OPERATION_NOT_ALLOWED",
                "message", "Operation not allowed for current reservation status"
            ));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            ReservationDocument document = reservationService.getDocument(id);
            ctx.json(toDocumentMap(document));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(Map.of(
                "error", "OPERATION_NOT_ALLOWED",
                "message", "Operation not allowed for current reservation status"
            ));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ReservationDocument document = reservationService.getDocument(id);
            ctx.json(toDocumentMap(document));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        }
    }

    private Map<String, Object> toMap(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = subtotal.subtract(discount);

        Map<String, Object> map = new HashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", reservation.getCheckIn().toString());
        map.put("checkOut", reservation.getCheckOut().toString());
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan().name());
        map.put("nights", reservation.getNights());
        map.put("subtotal", subtotal);
        map.put("discount", discount);
        map.put("total", total);
        map.put("status", reservation.getStatus().name());
        map.put("amountPaid", reservation.getAmountPaid());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private Map<String, Object> toDocumentMap(ReservationDocument doc) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", doc.getTitle());
        map.put("reservationId", doc.getReservationId());
        map.put("guestName", doc.getGuestName());
        map.put("guestCpf", doc.getGuestCpf());
        map.put("roomNumber", doc.getRoomNumber());
        map.put("roomType", doc.getRoomType());
        map.put("checkIn", doc.getCheckIn().toString());
        map.put("checkOut", doc.getCheckOut().toString());
        map.put("nights", doc.getNights());
        map.put("subtotal", doc.getSubtotal());
        map.put("discount", doc.getDiscount());
        map.put("total", doc.getTotal());
        return map;
    }
}
