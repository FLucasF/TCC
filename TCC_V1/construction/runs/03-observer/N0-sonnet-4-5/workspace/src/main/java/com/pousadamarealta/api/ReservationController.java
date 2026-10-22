package com.pousadamarealta.api;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale BRAZIL_LOCALE = new Locale("pt", "BR");

    public ReservationController(ReservationService reservationService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    public void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            String roomNumber = getRequiredString(body, "roomNumber");
            String guestName = getRequiredString(body, "guestName");
            String guestEmail = getRequiredString(body, "guestEmail");
            String guestCpf = getRequiredString(body, "guestCpf");
            LocalDate checkIn = parseDate(getRequiredString(body, "checkIn"));
            LocalDate checkOut = parseDate(getRequiredString(body, "checkOut"));
            int guests = getRequiredInt(body, "guests");
            RatePlan ratePlan = RatePlan.valueOf(getRequiredString(body, "ratePlan"));

            Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan);

            ctx.status(201).json(toReservationResponse(reservation));

        } catch (MissingFieldException | IllegalArgumentException | DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(toReservationResponse(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(toReservationResponse(reservation));
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
            ctx.json(toReservationResponse(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toReservationResponse(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            ctx.json(toDocumentResponse(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getAmountPaid().compareTo(BigDecimal.ZERO) == 0) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Document not available"));
                return;
            }

            ctx.json(toDocumentResponse(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    private Map<String, Object> toReservationResponse(Reservation reservation) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", reservation.getId());
        response.put("roomNumber", reservation.getRoomNumber());
        response.put("guestName", reservation.getGuestName());
        response.put("guestEmail", reservation.getGuestEmail());
        response.put("guestCpf", reservation.getGuestCpf());
        response.put("checkIn", reservation.getCheckIn().toString());
        response.put("checkOut", reservation.getCheckOut().toString());
        response.put("guests", reservation.getGuests());
        response.put("ratePlan", reservation.getRatePlan().name());
        response.put("nights", reservation.getNights());
        response.put("subtotal", reservation.getSubtotal());
        response.put("discount", reservation.getDiscount());
        response.put("total", reservation.getTotal());
        response.put("status", reservation.getStatus().name());
        response.put("amountPaid", reservation.getAmountPaid());
        response.put("refund", reservation.getRefund());
        return response;
    }

    private Map<String, Object> toDocumentResponse(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        Map<String, Object> response = new HashMap<>();
        response.put("title", "Comprovante de reserva");
        response.put("reservationId", reservation.getId());
        response.put("guestName", reservation.getGuestName());
        response.put("guestCpf", reservation.getGuestCpf());
        response.put("roomNumber", reservation.getRoomNumber());
        response.put("roomType", room.getType().name());
        response.put("checkIn", reservation.getCheckIn().toString());
        response.put("checkOut", reservation.getCheckOut().toString());
        response.put("nights", reservation.getNights());
        response.put("subtotal", reservation.getSubtotal());
        response.put("discount", reservation.getDiscount());
        response.put("total", reservation.getTotal());
        return response;
    }

    private String getRequiredString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new MissingFieldException("Missing required field: " + key);
        }
        return value.toString();
    }

    private int getRequiredInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new MissingFieldException("Missing required field: " + key);
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

    private static class MissingFieldException extends RuntimeException {
        public MissingFieldException(String message) {
            super(message);
        }
    }
}
