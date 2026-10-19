package com.pousadamarealta.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pousadamarealta.model.*;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.util.Formatter;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        try {
            JsonNode json = ctx.bodyAsClass(JsonNode.class);

            String roomNumber = getRequiredString(json, "roomNumber");
            String guestName = getRequiredString(json, "guestName");
            String guestEmail = getRequiredString(json, "guestEmail");
            String guestCpf = getRequiredString(json, "guestCpf");
            LocalDate checkIn = getRequiredDate(json, "checkIn");
            LocalDate checkOut = getRequiredDate(json, "checkOut");
            int guests = getRequiredInt(json, "guests");
            RatePlan ratePlan = getRequiredEnum(json, "ratePlan", RatePlan.class);

            Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(toReservationResponse(reservation));
        } catch (MissingFieldException | InvalidFormatException e) {
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
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.findById(id)
                    .orElseThrow(() -> new ReservationService.ReservationNotFoundException("Reservation not found"));
            ctx.json(toReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(toReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            Document document = reservationService.generateDocument(id);
            ctx.json(toDocumentResponse(document));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Document document = reservationService.generateDocument(id);
            ctx.json(toDocumentResponse(document));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    private Map<String, Object> toReservationResponse(Reservation r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("roomNumber", r.getRoomNumber());
        map.put("guestName", r.getGuestName());
        map.put("guestEmail", r.getGuestEmail());
        map.put("guestCpf", r.getGuestCpf());
        map.put("checkIn", r.getCheckIn().toString());
        map.put("checkOut", r.getCheckOut().toString());
        map.put("guests", r.getGuests());
        map.put("ratePlan", r.getRatePlan());
        map.put("nights", r.getNights());
        map.put("subtotal", r.getSubtotal());
        map.put("discount", r.getDiscount());
        map.put("total", r.getTotal());
        map.put("status", r.getStatus());
        map.put("amountPaid", r.getAmountPaid());
        map.put("refund", r.getRefund());
        return map;
    }

    private Map<String, Object> toDocumentResponse(Document d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", d.getTitle());
        map.put("reservationId", d.getReservationId());
        map.put("guestName", d.getGuestName());
        map.put("guestCpf", d.getGuestCpf());
        map.put("roomNumber", d.getRoomNumber());
        map.put("roomType", d.getRoomType());
        map.put("checkIn", d.getCheckIn().toString());
        map.put("checkOut", d.getCheckOut().toString());
        map.put("nights", d.getNights());
        map.put("subtotal", d.getSubtotal());
        map.put("discount", d.getDiscount());
        map.put("total", d.getTotal());
        return map;
    }

    private String getRequiredString(JsonNode json, String field) {
        if (!json.has(field) || json.get(field).isNull()) {
            throw new MissingFieldException("Missing required field: " + field);
        }
        return json.get(field).asText();
    }

    private int getRequiredInt(JsonNode json, String field) {
        if (!json.has(field) || json.get(field).isNull()) {
            throw new MissingFieldException("Missing required field: " + field);
        }
        return json.get(field).asInt();
    }

    private LocalDate getRequiredDate(JsonNode json, String field) {
        String dateStr = getRequiredString(json, field);
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new InvalidFormatException("Invalid date format for field: " + field);
        }
    }

    private <T extends Enum<T>> T getRequiredEnum(JsonNode json, String field, Class<T> enumClass) {
        String value = getRequiredString(json, field);
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new InvalidFormatException("Invalid value for field: " + field);
        }
    }

    private static class MissingFieldException extends RuntimeException {
        public MissingFieldException(String message) {
            super(message);
        }
    }

    private static class InvalidFormatException extends RuntimeException {
        public InvalidFormatException(String message) {
            super(message);
        }
    }
}
