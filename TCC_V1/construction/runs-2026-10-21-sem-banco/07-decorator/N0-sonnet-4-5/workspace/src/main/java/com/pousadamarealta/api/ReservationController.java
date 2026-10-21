package com.pousadamarealta.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReservationController(ReservationService reservationService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    public void createReservation(Context ctx) {
        try {
            JsonNode json = ctx.bodyAsClass(JsonNode.class);

            String roomNumber = getRequiredField(json, "roomNumber");
            String guestName = getRequiredField(json, "guestName");
            String guestEmail = getRequiredField(json, "guestEmail");
            String guestCpf = getRequiredField(json, "guestCpf");
            String checkInStr = getRequiredField(json, "checkIn");
            String checkOutStr = getRequiredField(json, "checkOut");
            int guests = getRequiredIntField(json, "guests");
            String ratePlanStr = getRequiredField(json, "ratePlan");

            LocalDate checkIn = LocalDate.parse(checkInStr, DATE_FORMATTER);
            LocalDate checkOut = LocalDate.parse(checkOutStr, DATE_FORMATTER);
            RatePlan ratePlan = RatePlan.valueOf(ratePlanStr);

            Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(toJson(reservation));

        } catch (MissingFieldException | DateTimeParseException | IllegalArgumentException e) {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Invalid request: " + e.getMessage()
            ));
        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(Map.of(
                    "error", "ROOM_NOT_FOUND",
                    "message", "Room not found"
            ));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(Map.of(
                    "error", "ROOM_UNAVAILABLE",
                    "message", "Room is not available for the selected dates"
            ));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(Map.of(
                    "error", "CAPACITY_EXCEEDED",
                    "message", "Number of guests exceeds room capacity"
            ));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Invalid request"
            ));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(toJson(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Invalid reservation ID"
            ));
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
            ctx.json(toJson(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Invalid reservation ID"
            ));
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

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toJson(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Invalid reservation ID"
            ));
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
            ctx.json(toJson(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Invalid reservation ID"
            ));
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
            String receipt = reservationService.checkOut(id);
            ctx.json(parseReceipt(id, receipt));
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Invalid reservation ID"
            ));
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
            String receipt = reservationService.getDocument(id);
            ctx.json(parseReceipt(id, receipt));
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Invalid reservation ID"
            ));
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

    private Map<String, Object> toJson(Reservation reservation) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", reservation.getCheckIn().format(DATE_FORMATTER));
        map.put("checkOut", reservation.getCheckOut().format(DATE_FORMATTER));
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        map.put("status", reservation.getStatus());
        map.put("amountPaid", reservation.getAmountPaid());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private Map<String, Object> parseReceipt(Long id, String receipt) {
        Reservation reservation = reservationService.getReservation(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        Map<String, Object> map = new HashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", reservation.getId());
        map.put("guestName", reservation.getGuestName());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("roomType", room.getType());
        map.put("checkIn", reservation.getCheckIn().format(DATE_FORMATTER));
        map.put("checkOut", reservation.getCheckOut().format(DATE_FORMATTER));
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        return map;
    }

    private String getRequiredField(JsonNode json, String field) throws MissingFieldException {
        if (!json.has(field) || json.get(field).isNull()) {
            throw new MissingFieldException("Missing required field: " + field);
        }
        return json.get(field).asText();
    }

    private int getRequiredIntField(JsonNode json, String field) throws MissingFieldException {
        if (!json.has(field) || json.get(field).isNull()) {
            throw new MissingFieldException("Missing required field: " + field);
        }
        return json.get(field).asInt();
    }

    private static class MissingFieldException extends Exception {
        public MissingFieldException(String message) {
            super(message);
        }
    }
}
