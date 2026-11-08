package com.pousada.marealto.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.marealto.domain.RatePlan;
import com.pousada.marealto.domain.Reservation;
import com.pousada.marealto.service.MessageService;
import com.pousada.marealto.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public ReservationController(ReservationService reservationService,
                                MessageService messageService,
                                ObjectMapper objectMapper) {
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    public void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            Reservation reservation = new Reservation();
            reservation.setRoomNumber(getRequiredString(body, "roomNumber"));
            reservation.setGuestName(getRequiredString(body, "guestName"));
            reservation.setGuestEmail(getRequiredString(body, "guestEmail"));
            reservation.setGuestCpf(getRequiredString(body, "guestCpf"));
            reservation.setCheckIn(parseDate(getRequiredString(body, "checkIn")));
            reservation.setCheckOut(parseDate(getRequiredString(body, "checkOut")));
            reservation.setGuests(getRequiredInt(body, "guests"));
            reservation.setRatePlan(RatePlan.valueOf(getRequiredString(body, "ratePlan")));

            if (body.containsKey("companyCode") && body.get("companyCode") != null) {
                reservation.setCompanyCode((String) body.get("companyCode"));
            }

            Reservation created = reservationService.createReservation(reservation);
            ctx.status(201).json(reservationToMap(created));
        } catch (IllegalArgumentException e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", e.getMessage());
        } catch (ReservationService.InvalidRequestException e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", e.getMessage());
        } catch (ReservationService.RoomUnavailableException e) {
            errorResponse(ctx, 409, "ROOM_UNAVAILABLE", e.getMessage());
        } catch (ReservationService.CapacityExceededException e) {
            errorResponse(ctx, 422, "CAPACITY_EXCEEDED", e.getMessage());
        } catch (Exception e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", "Invalid request data");
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(reservationToMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            errorResponse(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (NumberFormatException e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(reservationToMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            errorResponse(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (ReservationService.OperationNotAllowedException e) {
            errorResponse(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        } catch (NumberFormatException e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(reservationToMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            errorResponse(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (ReservationService.OperationNotAllowedException e) {
            errorResponse(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        } catch (NumberFormatException e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(reservationToMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            errorResponse(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (ReservationService.OperationNotAllowedException e) {
            errorResponse(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        } catch (NumberFormatException e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            ctx.json(documentToMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            errorResponse(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (ReservationService.OperationNotAllowedException e) {
            errorResponse(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        } catch (NumberFormatException e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getAmountPaid().compareTo(java.math.BigDecimal.ZERO) == 0) {
                errorResponse(ctx, 400, "INVALID_REQUEST", "Document only available for paid reservations");
                return;
            }

            ctx.json(documentToMap(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            errorResponse(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (NumberFormatException e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        }
    }

    private Map<String, Object> reservationToMap(Reservation r) {
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

    private Map<String, Object> documentToMap(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", r.getId());
        map.put("guestName", r.getGuestName());
        map.put("guestCpf", r.getGuestCpf());
        map.put("roomNumber", r.getRoomNumber());
        map.put("roomType", getRoomType(r.getRoomNumber()));
        map.put("checkIn", r.getCheckIn().toString());
        map.put("checkOut", r.getCheckOut().toString());
        map.put("nights", r.getNights());
        map.put("subtotal", r.getSubtotal());
        map.put("discount", r.getDiscount());
        map.put("total", r.getTotal());
        return map;
    }

    private String getRoomType(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "UNKNOWN";
        };
    }

    private String getRequiredString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return value.toString();
    }

    private int getRequiredInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr);
        }
    }

    private void errorResponse(Context ctx, int status, String code, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", code);
        error.put("message", message);
        ctx.status(status).json(error);
    }
}
