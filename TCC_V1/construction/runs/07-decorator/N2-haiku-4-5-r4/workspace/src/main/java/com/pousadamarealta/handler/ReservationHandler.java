package com.pousadamarealta.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationHandler {
    private final ReservationService reservationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReservationHandler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        try {
            Map<String, Object> body = objectMapper.readValue(ctx.body(), Map.class);

            Integer roomNumber = ((Number) body.get("roomNumber")).intValue();
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String guestCpf = (String) body.get("guestCpf");
            String checkInStr = (String) body.get("checkIn");
            String checkOutStr = (String) body.get("checkOut");
            Integer guests = ((Number) body.get("guests")).intValue();
            String ratePlan = (String) body.get("ratePlan");

            if (roomNumber == null || guestName == null || guestEmail == null || guestCpf == null ||
                checkInStr == null || checkOutStr == null || guests == null || ratePlan == null) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Missing required fields"));
                return;
            }

            LocalDate checkIn = LocalDate.parse(checkInStr);
            LocalDate checkOut = LocalDate.parse(checkOutStr);

            try {
                Reservation reservation = reservationService.createReservation(
                        roomNumber, guestName, guestEmail, guestCpf,
                        checkIn, checkOut, guests, ratePlan
                );
                ctx.status(201).json(reservationToJson(reservation));
            } catch (RuntimeException e) {
                handleError(ctx, e.getMessage());
            }
        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
            } else {
                ctx.json(reservationToJson(reservation));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(reservationToJson(reservation));
        } catch (RuntimeException e) {
            handleError(ctx, e.getMessage());
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(reservationToJson(reservation));
        } catch (RuntimeException e) {
            handleError(ctx, e.getMessage());
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(reservationToJson(reservation));
        } catch (RuntimeException e) {
            handleError(ctx, e.getMessage());
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            String receipt = reservationService.getReceipt(id);

            Map<String, Object> response = new HashMap<>(reservationToJson(reservation));
            response.put("document", receipt);

            ctx.json(response);
        } catch (RuntimeException e) {
            handleError(ctx, e.getMessage());
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservationById(id);
            if (reservation == null) {
                ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                return;
            }

            String receipt = reservationService.getReceipt(id);

            Map<String, Object> document = new HashMap<>();
            document.put("title", "Comprovante de reserva");
            document.put("reservationId", reservation.id);
            document.put("guestName", reservation.guestName);
            document.put("guestCpf", formatCpf(reservation.guestCpf));
            document.put("roomNumber", reservation.roomNumber);
            document.put("roomType", reservation.roomNumber == 101 || reservation.roomNumber == 102 ? "STANDARD" :
                                      reservation.roomNumber == 201 || reservation.roomNumber == 202 ? "DELUXE" : "SUITE");
            document.put("checkIn", formatDateForJson(reservation.checkIn));
            document.put("checkOut", formatDateForJson(reservation.checkOut));
            document.put("nights", reservation.nights);
            document.put("subtotal", formatMoney(reservation.subtotal));
            document.put("discount", formatMoney(reservation.discount));
            document.put("total", formatMoney(reservation.total));

            ctx.json(document);
        } catch (RuntimeException e) {
            handleError(ctx, e.getMessage());
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    private Map<String, Object> reservationToJson(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.id);
        map.put("roomNumber", r.roomNumber);
        map.put("guestName", r.guestName);
        map.put("guestEmail", r.guestEmail);
        map.put("guestCpf", r.guestCpf);
        map.put("checkIn", formatDateForJson(r.checkIn));
        map.put("checkOut", formatDateForJson(r.checkOut));
        map.put("guests", r.guests);
        map.put("ratePlan", r.ratePlan.name());
        map.put("nights", r.nights);
        map.put("subtotal", formatMoney(r.subtotal));
        map.put("discount", formatMoney(r.discount));
        map.put("total", formatMoney(r.total));
        map.put("status", r.status.name());
        map.put("amountPaid", formatMoney(r.amountPaid));
        if (r.refund != null) {
            map.put("refund", formatMoney(r.refund));
        } else {
            map.put("refund", null);
        }
        return map;
    }

    private String formatMoney(java.math.BigDecimal amount) {
        return String.format("%.2f", amount);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatDateForJson(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private void handleError(Context ctx, String error) {
        switch (error) {
            case "ROOM_NOT_FOUND":
                ctx.status(404).json(Map.of("error", "ROOM_NOT_FOUND", "message", "Room not found"));
                break;
            case "RESERVATION_NOT_FOUND":
                ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                break;
            case "ROOM_UNAVAILABLE":
                ctx.status(409).json(Map.of("error", "ROOM_UNAVAILABLE", "message", "Room is unavailable for these dates"));
                break;
            case "OPERATION_NOT_ALLOWED":
                ctx.status(409).json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "This operation is not allowed for this reservation"));
                break;
            case "CAPACITY_EXCEEDED":
                ctx.status(422).json(Map.of("error", "CAPACITY_EXCEEDED", "message", "Number of guests exceeds room capacity"));
                break;
            case "INVALID_REQUEST":
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid request"));
                break;
            default:
                ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", error));
        }
    }
}
