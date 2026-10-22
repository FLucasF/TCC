package com.pousada.marealta.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.pousada.marealta.domain.RatePlan;
import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.infrastructure.persistence.ReservationRepository;
import com.pousada.marealta.infrastructure.persistence.RoomRepository;
import com.pousada.marealta.service.ReservationService;
import com.pousada.marealta.util.DateFormatter;
import io.javalin.Javalin;

import java.time.LocalDate;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final Gson gson = new GsonBuilder().create();

    public ReservationController(ReservationService reservationService, ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public void register(Javalin app) {
        app.post("/reservations", ctx -> {
            try {
                JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

                if (body == null || !body.has("roomNumber") || !body.has("guestName") ||
                    !body.has("guestEmail") || !body.has("guestCpf") ||
                    !body.has("checkIn") || !body.has("checkOut") ||
                    !body.has("guests") || !body.has("ratePlan")) {
                    ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Missing required fields"));
                    return;
                }

                int roomNumber = body.get("roomNumber").getAsInt();
                String guestName = body.get("guestName").getAsString();
                String guestEmail = body.get("guestEmail").getAsString();
                String guestCpf = body.get("guestCpf").getAsString();
                String checkInStr = body.get("checkIn").getAsString();
                String checkOutStr = body.get("checkOut").getAsString();
                int guests = body.get("guests").getAsInt();
                String ratePlanStr = body.get("ratePlan").getAsString();

                try {
                    LocalDate checkIn = DateFormatter.parseISO(checkInStr);
                    LocalDate checkOut = DateFormatter.parseISO(checkOutStr);
                    RatePlan ratePlan = RatePlan.fromString(ratePlanStr);

                    Reservation reservation = reservationService.createReservation(
                        roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan);

                    ctx.status(201).json(reservationToJson(reservation));
                } catch (IllegalArgumentException e) {
                    handleReservationError(ctx, e.getMessage());
                }
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.get("/reservations/:id", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                    return;
                }

                ctx.json(reservationToJson(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
            }
        });

        app.post("/reservations/:id/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.confirmReservation(id);
                ctx.json(reservationToJson(reservation));
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                } else {
                    ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
                }
            } catch (IllegalStateException e) {
                ctx.status(409).json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
            }
        });

        app.post("/reservations/:id/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(reservationToJson(reservation));
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                } else {
                    ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
                }
            } catch (IllegalStateException e) {
                ctx.status(409).json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
            }
        });

        app.post("/reservations/:id/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(reservationToJson(reservation));
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                } else {
                    ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
                }
            } catch (IllegalStateException e) {
                ctx.status(409).json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
            }
        });

        app.post("/reservations/:id/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkOut(id);

                var room = roomRepository.findByNumber(reservation.getRoomNumber());
                JsonObject receiptJson = new JsonObject();
                receiptJson.addProperty("title", "Comprovante de reserva");
                receiptJson.addProperty("reservationId", reservation.getId());
                receiptJson.addProperty("guestName", reservation.getGuestName());
                receiptJson.addProperty("guestCpf", reservation.getGuestCpf());
                receiptJson.addProperty("roomNumber", reservation.getRoomNumber());
                receiptJson.addProperty("roomType", room.getType().getName());
                receiptJson.addProperty("checkIn", DateFormatter.toISO(reservation.getCheckIn()));
                receiptJson.addProperty("checkOut", DateFormatter.toISO(reservation.getCheckOut()));
                receiptJson.addProperty("nights", reservation.getNights());
                receiptJson.addProperty("subtotal", reservation.getSubtotal());
                receiptJson.addProperty("discount", reservation.getDiscount());
                receiptJson.addProperty("total", reservation.getTotal());

                ctx.json(receiptJson);
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                } else {
                    ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
                }
            } catch (IllegalStateException e) {
                ctx.status(409).json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
            }
        });

        app.get("/reservations/:id/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationRepository.findById(id);

                if (reservation == null) {
                    ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                    return;
                }

                var room = roomRepository.findByNumber(reservation.getRoomNumber());
                JsonObject receiptJson = new JsonObject();
                receiptJson.addProperty("title", "Comprovante de reserva");
                receiptJson.addProperty("reservationId", reservation.getId());
                receiptJson.addProperty("guestName", reservation.getGuestName());
                receiptJson.addProperty("guestCpf", reservation.getGuestCpf());
                receiptJson.addProperty("roomNumber", reservation.getRoomNumber());
                receiptJson.addProperty("roomType", room.getType().getName());
                receiptJson.addProperty("checkIn", DateFormatter.toISO(reservation.getCheckIn()));
                receiptJson.addProperty("checkOut", DateFormatter.toISO(reservation.getCheckOut()));
                receiptJson.addProperty("nights", reservation.getNights());
                receiptJson.addProperty("subtotal", reservation.getSubtotal());
                receiptJson.addProperty("discount", reservation.getDiscount());
                receiptJson.addProperty("total", reservation.getTotal());

                ctx.json(receiptJson);
            } catch (NumberFormatException e) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
            }
        });
    }

    private Map<String, Object> reservationToJson(Reservation reservation) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", DateFormatter.toISO(reservation.getCheckIn()));
        map.put("checkOut", DateFormatter.toISO(reservation.getCheckOut()));
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

    private void handleReservationError(io.javalin.http.Context ctx, String message) {
        if (message.contains("Room not found")) {
            ctx.status(404).json(Map.of("error", "ROOM_NOT_FOUND", "message", "Room not found"));
        } else if (message.contains("Invalid guest count")) {
            ctx.status(422).json(Map.of("error", "CAPACITY_EXCEEDED", "message", "Invalid guest count"));
        } else if (message.contains("Invalid dates")) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid dates"));
        } else if (message.contains("Room unavailable")) {
            ctx.status(409).json(Map.of("error", "ROOM_UNAVAILABLE", "message", "Room unavailable"));
        } else {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", message));
        }
    }
}
