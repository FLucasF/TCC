package com.pousada.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationDocument;
import com.pousada.service.ReservationService;
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
            JsonNode json = ctx.bodyAsClass(JsonNode.class);

            String roomNumber = getRequiredField(json, "roomNumber").asText();
            String guestName = getRequiredField(json, "guestName").asText();
            String guestEmail = getRequiredField(json, "guestEmail").asText();
            String guestCpf = getRequiredField(json, "guestCpf").asText();
            LocalDate checkIn = parseDate(getRequiredField(json, "checkIn").asText());
            LocalDate checkOut = parseDate(getRequiredField(json, "checkOut").asText());
            int guests = getRequiredField(json, "guests").asInt();
            RatePlan ratePlan = RatePlan.valueOf(getRequiredField(json, "ratePlan").asText());

            if (!checkOut.isAfter(checkIn)) {
                throw new InvalidRequestException("Check-out must be after check-in");
            }

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(toMap(reservation));

        } catch (NullPointerException | IllegalArgumentException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request data"));
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
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
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(toMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(toMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkInReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkInReservation(id);
            ctx.json(toMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkOutReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ReservationDocument document = reservationService.checkOutReservation(id);
            ctx.json(toDocumentMap(document));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ReservationDocument document = reservationService.getDocument(id);
            ctx.json(toDocumentMap(document));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    private JsonNode getRequiredField(JsonNode json, String fieldName) {
        JsonNode field = json.get(fieldName);
        if (field == null || field.isNull()) {
            throw new NullPointerException("Missing required field: " + fieldName);
        }
        return field;
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException("Invalid date format", date, 0);
        }
    }

    private Map<String, Object> toMap(Reservation reservation) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", reservation.getCheckIn());
        map.put("checkOut", reservation.getCheckOut());
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan().name());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        map.put("status", reservation.getStatus().name());
        map.put("amountPaid", reservation.getAmountPaid());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private Map<String, Object> toDocumentMap(ReservationDocument document) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", document.getTitle());
        map.put("reservationId", document.getReservationId());
        map.put("guestName", document.getGuestName());
        map.put("guestCpf", document.getGuestCpf());
        map.put("roomNumber", document.getRoomNumber());
        map.put("roomType", document.getRoomType().name());
        map.put("checkIn", document.getCheckIn());
        map.put("checkOut", document.getCheckOut());
        map.put("nights", document.getNights());
        map.put("subtotal", document.getSubtotal());
        map.put("discount", document.getDiscount());
        map.put("total", document.getTotal());
        return map;
    }

    private static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}
