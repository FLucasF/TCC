package br.marealta.api;

import br.marealta.model.RatePlan;
import br.marealta.model.Reservation;
import br.marealta.service.ReservationService;
import br.marealta.util.Formatters;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
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

            int roomNumber = ((Number) body.get("roomNumber")).intValue();
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String guestCpf = (String) body.get("guestCpf");
            String checkInStr = (String) body.get("checkIn");
            String checkOutStr = (String) body.get("checkOut");
            int guests = ((Number) body.get("guests")).intValue();
            String ratePlanStr = (String) body.get("ratePlan");

            validateCreateReservationInput(roomNumber, guestName, guestEmail, guestCpf,
                checkInStr, checkOutStr, guests, ratePlanStr);

            LocalDate checkIn = Formatters.parseIsoDate(checkInStr);
            LocalDate checkOut = Formatters.parseIsoDate(checkOutStr);
            RatePlan ratePlan = RatePlan.valueOf(ratePlanStr);

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan);

            ctx.status(201).json(toResponse(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid request body"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(toResponse(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            if (!body.containsKey("amountPaid")) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Missing amountPaid"));
                return;
            }

            BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());

            Reservation reservation = reservationService.confirmReservation(id, amountPaid);
            ctx.json(toResponse(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid request body"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toResponse(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toResponse(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.checkOut(id);
            String document = reservationService.generateConfirmation(reservation);
            Map<String, Object> response = new HashMap<>();
            response.put("title", "Comprovante de reserva");
            response.put("reservationId", reservation.getId());
            response.put("guestName", reservation.getGuestName());
            response.put("guestCpf", reservation.getGuestCpf());
            response.put("roomNumber", reservation.getRoomNumber());
            response.put("roomType", reservation.getRoomNumber());
            response.put("checkIn", Formatters.formatDate(reservation.getCheckIn()));
            response.put("checkOut", Formatters.formatDate(reservation.getCheckOut()));
            response.put("nights", reservation.getNights());
            response.put("subtotal", reservation.getSubtotal().doubleValue());
            response.put("discount", reservation.getDiscount().doubleValue());
            response.put("total", reservation.getTotal().doubleValue());
            ctx.json(response);
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getAmountPaid().compareTo(java.math.BigDecimal.ZERO) == 0) {
                ctx.status(409).json(Map.of("error", "OPERATION_NOT_ALLOWED",
                    "message", "Document only available for paid reservations"));
                return;
            }

            String document = reservationService.generateConfirmation(reservation);
            Map<String, Object> response = new HashMap<>();
            response.put("title", "Comprovante de reserva");
            response.put("reservationId", reservation.getId());
            response.put("guestName", reservation.getGuestName());
            response.put("guestCpf", reservation.getGuestCpf());
            response.put("roomNumber", reservation.getRoomNumber());
            response.put("roomType", reservation.getRoomNumber());
            response.put("checkIn", Formatters.formatDate(reservation.getCheckIn()));
            response.put("checkOut", Formatters.formatDate(reservation.getCheckOut()));
            response.put("nights", reservation.getNights());
            response.put("subtotal", reservation.getSubtotal().doubleValue());
            response.put("discount", reservation.getDiscount().doubleValue());
            response.put("total", reservation.getTotal().doubleValue());
            ctx.json(response);
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        }
    }

    private Map<String, Object> toResponse(Reservation reservation) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", Formatters.formatDate(reservation.getCheckIn()));
        map.put("checkOut", Formatters.formatDate(reservation.getCheckOut()));
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan().name());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal().doubleValue());
        map.put("discount", reservation.getDiscount().doubleValue());
        map.put("total", reservation.getTotal().doubleValue());
        map.put("status", reservation.getStatus().name());
        map.put("amountPaid", reservation.getAmountPaid().doubleValue());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private void validateCreateReservationInput(int roomNumber, String guestName, String guestEmail,
                                               String guestCpf, String checkIn, String checkOut,
                                               int guests, String ratePlan) {
        if (roomNumber <= 0 || guestName == null || guestName.isEmpty() ||
            guestEmail == null || guestEmail.isEmpty() || guestCpf == null || guestCpf.isEmpty() ||
            checkIn == null || checkOut == null || guests <= 0 || ratePlan == null) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        try {
            Formatters.parseIsoDate(checkIn);
            Formatters.parseIsoDate(checkOut);
            RatePlan.valueOf(ratePlan);
        } catch (Exception e) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
    }

    private void handleError(Context ctx, String errorCode) {
        switch (errorCode) {
            case "ROOM_NOT_FOUND":
                ctx.status(404).json(Map.of("error", "ROOM_NOT_FOUND", "message", "Room not found"));
                break;
            case "RESERVATION_NOT_FOUND":
                ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                break;
            case "ROOM_UNAVAILABLE":
                ctx.status(409).json(Map.of("error", "ROOM_UNAVAILABLE", "message", "Room is unavailable for selected dates"));
                break;
            case "CAPACITY_EXCEEDED":
                ctx.status(422).json(Map.of("error", "CAPACITY_EXCEEDED", "message", "Number of guests exceeds room capacity"));
                break;
            case "OPERATION_NOT_ALLOWED":
                ctx.status(409).json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Operation not allowed for this reservation status"));
                break;
            case "INVALID_REQUEST":
            default:
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid request"));
                break;
        }
    }
}
