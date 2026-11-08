package com.pousadamarealta.controller;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.util.FormatUtil;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            String roomNumber = (String) body.get("roomNumber");
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String guestCpf = (String) body.get("guestCpf");
            String checkInStr = (String) body.get("checkIn");
            String checkOutStr = (String) body.get("checkOut");
            Integer guests = ((Number) body.get("guests")).intValue();
            String ratePlanStr = (String) body.get("ratePlan");

            if (roomNumber == null || guestName == null || guestEmail == null ||
                guestCpf == null || checkInStr == null || checkOutStr == null ||
                guests == null || ratePlanStr == null) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Missing required fields"
                ));
                return;
            }

            if (guests < 1) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "At least one guest is required"
                ));
                return;
            }

            LocalDate checkIn = LocalDate.parse(checkInStr);
            LocalDate checkOut = LocalDate.parse(checkOutStr);

            if (!checkOut.isAfter(checkIn)) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Check-out must be after check-in"
                ));
                return;
            }

            RatePlan ratePlan = RatePlan.valueOf(ratePlanStr);

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(toMap(reservation));

        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", "Invalid date format"
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", "Invalid rate plan"
            ));
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

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(toMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(toMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(Map.of(
                "error", "OPERATION_NOT_ALLOWED",
                "message", "Cannot confirm reservation in current status"
            ));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(Map.of(
                "error", "OPERATION_NOT_ALLOWED",
                "message", "Cannot cancel reservation in current status"
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
                "message", "Cannot check-in reservation in current status"
            ));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);

            Map<String, Object> receipt = new HashMap<>();
            receipt.put("title", "Comprovante de reserva");
            receipt.put("reservationId", reservation.getId());
            receipt.put("guestName", reservation.getGuestName());
            receipt.put("guestCpf", reservation.getGuestCpf());
            receipt.put("roomNumber", reservation.getRoomNumber());
            receipt.put("roomType", getRoomType(reservation.getRoomNumber()));
            receipt.put("checkIn", reservation.getCheckIn().toString());
            receipt.put("checkOut", reservation.getCheckOut().toString());
            receipt.put("nights", reservation.getNights());
            receipt.put("subtotal", reservation.getSubtotal());
            receipt.put("discount", reservation.getDiscount());
            receipt.put("total", reservation.getTotal());

            ctx.json(receipt);
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(Map.of(
                "error", "OPERATION_NOT_ALLOWED",
                "message", "Cannot check-out reservation in current status"
            ));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getAmountPaid().signum() == 0) {
                ctx.status(409).json(Map.of(
                    "error", "OPERATION_NOT_ALLOWED",
                    "message", "No receipt available for unpaid reservation"
                ));
                return;
            }

            Map<String, Object> receipt = new HashMap<>();
            receipt.put("title", "Comprovante de reserva");
            receipt.put("reservationId", reservation.getId());
            receipt.put("guestName", reservation.getGuestName());
            receipt.put("guestCpf", reservation.getGuestCpf());
            receipt.put("roomNumber", reservation.getRoomNumber());
            receipt.put("roomType", getRoomType(reservation.getRoomNumber()));
            receipt.put("checkIn", reservation.getCheckIn().toString());
            receipt.put("checkOut", reservation.getCheckOut().toString());
            receipt.put("nights", reservation.getNights());
            receipt.put("subtotal", reservation.getSubtotal());
            receipt.put("discount", reservation.getDiscount());
            receipt.put("total", reservation.getTotal());

            ctx.json(receipt);
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(Map.of(
                "error", "RESERVATION_NOT_FOUND",
                "message", "Reservation not found"
            ));
        }
    }

    private Map<String, Object> toMap(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("roomNumber", r.getRoomNumber());
        map.put("guestName", r.getGuestName());
        map.put("guestEmail", r.getGuestEmail());
        map.put("guestCpf", r.getGuestCpf());
        map.put("checkIn", r.getCheckIn().toString());
        map.put("checkOut", r.getCheckOut().toString());
        map.put("guests", r.getGuests());
        map.put("ratePlan", r.getRatePlan().name());
        map.put("nights", r.getNights());
        map.put("subtotal", r.getSubtotal());
        map.put("discount", r.getDiscount());
        map.put("total", r.getTotal());
        map.put("status", r.getStatus().name());
        map.put("amountPaid", r.getAmountPaid());
        map.put("refund", r.getRefund());
        return map;
    }

    private String getRoomType(String roomNumber) {
        if (roomNumber.startsWith("1")) return "STANDARD";
        if (roomNumber.startsWith("2")) return "DELUXE";
        return "SUITE";
    }
}
