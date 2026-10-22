package com.pousada.api;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.repo.ReservationRepository;
import com.pousada.repo.RoomRepository;
import com.pousada.service.ReservationService;
import com.pousada.service.ValidationException;
import io.javalin.Javalin;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationRoutes {
    private static RoomRepository roomRepo;

    public static void register(Javalin app, ReservationService reservationService, ReservationRepository reservationRepo) {
        app.post("/reservations", ctx -> {
            try {
                String body = ctx.body();
                if (body == null || body.isEmpty()) {
                    ctx.status(400);
                    ctx.json(new JSONObject()
                        .put("error", "INVALID_REQUEST")
                        .put("message", "Request body is required")
                        .toString());
                    return;
                }

                JSONObject json = new JSONObject(body);

                if (!json.has("roomNumber") || !json.has("guestName") || !json.has("guestEmail") || !json.has("guestCpf") ||
                    !json.has("checkIn") || !json.has("checkOut") || !json.has("guests") || !json.has("ratePlan")) {
                    ctx.status(400);
                    ctx.json(new JSONObject()
                        .put("error", "INVALID_REQUEST")
                        .put("message", "Missing required fields")
                        .toString());
                    return;
                }

                int roomNumber = json.getInt("roomNumber");
                String guestName = json.getString("guestName");
                String guestEmail = json.getString("guestEmail");
                String guestCpf = json.getString("guestCpf");
                LocalDate checkIn = LocalDate.parse(json.getString("checkIn"));
                LocalDate checkOut = LocalDate.parse(json.getString("checkOut"));
                int guests = json.getInt("guests");
                RatePlan ratePlan = RatePlan.valueOf(json.getString("ratePlan"));

                Reservation reservation = reservationService.createReservation(roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan);

                ctx.status(201);
                ctx.json(reservationToJson(reservation).toString());
            } catch (ValidationException e) {
                int status = getStatusForError(e.getErrorCode());
                ctx.status(status);
                ctx.json(new JSONObject()
                    .put("error", e.getErrorCode())
                    .put("message", e.getMessage())
                    .toString());
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new JSONObject()
                    .put("error", "INVALID_REQUEST")
                    .put("message", "Invalid request: " + e.getMessage())
                    .toString());
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                ctx.json(reservationToJson(reservation).toString());
            } catch (ValidationException e) {
                ctx.status(404);
                ctx.json(new JSONObject()
                    .put("error", e.getErrorCode())
                    .put("message", e.getMessage())
                    .toString());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new JSONObject()
                    .put("error", "INTERNAL_ERROR")
                    .put("message", e.getMessage())
                    .toString());
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                String body = ctx.body();
                JSONObject json = new JSONObject(body);
                BigDecimal amountPaid = json.getBigDecimal("amountPaid");

                Reservation reservation = reservationService.confirmReservation(id, amountPaid);
                ctx.json(reservationToJson(reservation).toString());
            } catch (ValidationException e) {
                int status = getStatusForError(e.getErrorCode());
                ctx.status(status);
                ctx.json(new JSONObject()
                    .put("error", e.getErrorCode())
                    .put("message", e.getMessage())
                    .toString());
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new JSONObject()
                    .put("error", "INVALID_REQUEST")
                    .put("message", "Invalid request: " + e.getMessage())
                    .toString());
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(reservationToJson(reservation).toString());
            } catch (ValidationException e) {
                int status = getStatusForError(e.getErrorCode());
                ctx.status(status);
                ctx.json(new JSONObject()
                    .put("error", e.getErrorCode())
                    .put("message", e.getMessage())
                    .toString());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new JSONObject()
                    .put("error", "INTERNAL_ERROR")
                    .put("message", e.getMessage())
                    .toString());
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(reservationToJson(reservation).toString());
            } catch (ValidationException e) {
                int status = getStatusForError(e.getErrorCode());
                ctx.status(status);
                ctx.json(new JSONObject()
                    .put("error", e.getErrorCode())
                    .put("message", e.getMessage())
                    .toString());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new JSONObject()
                    .put("error", "INTERNAL_ERROR")
                    .put("message", e.getMessage())
                    .toString());
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkOut(id);

                Room room = getRoomRepository().findByNumber(reservation.getRoomNumber());
                String receipt = reservationService.buildReceipt(reservation, room);

                JSONObject receiptJson = new JSONObject()
                    .put("title", "Comprovante de reserva")
                    .put("reservationId", reservation.getId())
                    .put("guestName", reservation.getGuestName())
                    .put("guestCpf", reservation.getGuestCpf())
                    .put("roomNumber", reservation.getRoomNumber())
                    .put("roomType", room.getType().getDisplayName())
                    .put("checkIn", reservation.getCheckIn().toString())
                    .put("checkOut", reservation.getCheckOut().toString())
                    .put("nights", reservation.getNights())
                    .put("subtotal", reservation.getSubtotal().doubleValue())
                    .put("discount", reservation.getDiscount().doubleValue())
                    .put("total", reservation.getTotal().doubleValue());

                ctx.json(receiptJson.toString());
            } catch (ValidationException e) {
                int status = getStatusForError(e.getErrorCode());
                ctx.status(status);
                ctx.json(new JSONObject()
                    .put("error", e.getErrorCode())
                    .put("message", e.getMessage())
                    .toString());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new JSONObject()
                    .put("error", "INTERNAL_ERROR")
                    .put("message", e.getMessage())
                    .toString());
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (!reservation.isPaid()) {
                    ctx.status(404);
                    ctx.json(new JSONObject()
                        .put("error", "RESERVATION_NOT_FOUND")
                        .put("message", "Document is only available for paid reservations")
                        .toString());
                    return;
                }

                Room room = getRoomRepository().findByNumber(reservation.getRoomNumber());
                JSONObject receiptJson = new JSONObject()
                    .put("title", "Comprovante de reserva")
                    .put("reservationId", reservation.getId())
                    .put("guestName", reservation.getGuestName())
                    .put("guestCpf", reservation.getGuestCpf())
                    .put("roomNumber", reservation.getRoomNumber())
                    .put("roomType", room.getType().getDisplayName())
                    .put("checkIn", reservation.getCheckIn().toString())
                    .put("checkOut", reservation.getCheckOut().toString())
                    .put("nights", reservation.getNights())
                    .put("subtotal", reservation.getSubtotal().doubleValue())
                    .put("discount", reservation.getDiscount().doubleValue())
                    .put("total", reservation.getTotal().doubleValue());

                ctx.json(receiptJson.toString());
            } catch (ValidationException e) {
                ctx.status(404);
                ctx.json(new JSONObject()
                    .put("error", e.getErrorCode())
                    .put("message", e.getMessage())
                    .toString());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new JSONObject()
                    .put("error", "INTERNAL_ERROR")
                    .put("message", e.getMessage())
                    .toString());
            }
        });
    }

    private static JSONObject reservationToJson(Reservation res) {
        return new JSONObject()
            .put("id", res.getId())
            .put("roomNumber", res.getRoomNumber())
            .put("guestName", res.getGuestName())
            .put("guestEmail", res.getGuestEmail())
            .put("guestCpf", res.getGuestCpf())
            .put("checkIn", res.getCheckIn().toString())
            .put("checkOut", res.getCheckOut().toString())
            .put("guests", res.getGuests())
            .put("ratePlan", res.getRatePlan().getDisplayName())
            .put("nights", res.getNights())
            .put("subtotal", res.getSubtotal().doubleValue())
            .put("discount", res.getDiscount().doubleValue())
            .put("total", res.getTotal().doubleValue())
            .put("status", res.getStatus().getDisplayName())
            .put("amountPaid", res.getAmountPaid().doubleValue())
            .put("refund", res.getRefund() != null ? res.getRefund().doubleValue() : null);
    }

    private static int getStatusForError(String errorCode) {
        return switch(errorCode) {
            case "ROOM_NOT_FOUND", "RESERVATION_NOT_FOUND" -> 404;
            case "ROOM_UNAVAILABLE", "OPERATION_NOT_ALLOWED" -> 409;
            case "CAPACITY_EXCEEDED" -> 422;
            default -> 400;
        };
    }

    private static RoomRepository getRoomRepository() throws Exception {
        if (roomRepo == null) {
            roomRepo = new RoomRepository();
        }
        return roomRepo;
    }
}
