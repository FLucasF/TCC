package com.pousadamarealta.api;

import com.google.gson.JsonObject;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.services.Database;
import com.pousadamarealta.services.DocumentService;
import com.pousadamarealta.services.ReservationService;
import io.javalin.Javalin;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationsController {
    public static void register(Javalin app, ReservationService reservationService, DocumentService documentService, Database database) {
        app.post("/reservations", ctx -> {
            try {
                var body = ctx.bodyAsClass(java.util.Map.class);

                if (!body.containsKey("roomNumber") || !body.containsKey("guestName") ||
                    !body.containsKey("guestEmail") || !body.containsKey("guestCpf") ||
                    !body.containsKey("checkIn") || !body.containsKey("checkOut") ||
                    !body.containsKey("guests") || !body.containsKey("ratePlan")) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "INVALID_REQUEST");
                    error.addProperty("message", "Missing required fields");
                    ctx.status(400).json(error);
                    return;
                }

                int roomNumber = ((Number) body.get("roomNumber")).intValue();
                String guestName = (String) body.get("guestName");
                String guestEmail = (String) body.get("guestEmail");
                String guestCpf = (String) body.get("guestCpf");
                LocalDate checkIn = LocalDate.parse((String) body.get("checkIn"));
                LocalDate checkOut = LocalDate.parse((String) body.get("checkOut"));
                int guests = ((Number) body.get("guests")).intValue();
                RatePlan ratePlan = RatePlan.fromValue((String) body.get("ratePlan"));

                Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan
                );

                ctx.status(201).json(reservationToJson(reservation));
            } catch (DateTimeParseException e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "Invalid date format");
                ctx.status(400).json(error);
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", e.getMessage());
                ctx.status(400).json(error);
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = database.getReservationById(id);

                if (reservation.isEmpty()) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "RESERVATION_NOT_FOUND");
                    error.addProperty("message", "Reservation not found");
                    ctx.status(404).json(error);
                    return;
                }

                ctx.json(reservationToJson(reservation.get()));
            } catch (NumberFormatException e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "Invalid reservation ID");
                ctx.status(400).json(error);
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.confirmReservation(id);
                ctx.json(reservationToJson(reservation));
            } catch (NumberFormatException e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "Invalid reservation ID");
                ctx.status(400).json(error);
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(reservationToJson(reservation));
            } catch (NumberFormatException e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "Invalid reservation ID");
                ctx.status(400).json(error);
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(reservationToJson(reservation));
            } catch (NumberFormatException e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "Invalid reservation ID");
                ctx.status(400).json(error);
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkOut(id);
                var document = documentService.createDocument(reservation);
                var documentJson = new JsonObject();
                documentJson.addProperty("title", "Comprovante de reserva");
                documentJson.addProperty("reservationId", document.getReservationId());
                documentJson.addProperty("guestName", document.getGuestName());
                documentJson.addProperty("guestCpf", formatCpf(document.getGuestCpf()));
                documentJson.addProperty("roomNumber", document.getRoomNumber());
                documentJson.addProperty("roomType", document.getRoomType().getValue());
                documentJson.addProperty("checkIn", document.getCheckIn().toString());
                documentJson.addProperty("checkOut", document.getCheckOut().toString());
                documentJson.addProperty("nights", document.getNights());
                documentJson.addProperty("subtotal", document.getSubtotal().doubleValue());
                documentJson.addProperty("discount", document.getDiscount().doubleValue());
                documentJson.addProperty("total", document.getTotal().doubleValue());
                ctx.json(documentJson);
            } catch (NumberFormatException e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "Invalid reservation ID");
                ctx.status(400).json(error);
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = database.getReservationById(id);

                if (reservation.isEmpty()) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "RESERVATION_NOT_FOUND");
                    error.addProperty("message", "Reservation not found");
                    ctx.status(404).json(error);
                    return;
                }

                if (reservation.get().getAmountPaid().signum() == 0) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "OPERATION_NOT_ALLOWED");
                    error.addProperty("message", "Document only available for paid reservations");
                    ctx.status(409).json(error);
                    return;
                }

                var document = documentService.createDocument(reservation.get());
                var documentJson = new JsonObject();
                documentJson.addProperty("title", "Comprovante de reserva");
                documentJson.addProperty("reservationId", document.getReservationId());
                documentJson.addProperty("guestName", document.getGuestName());
                documentJson.addProperty("guestCpf", formatCpf(document.getGuestCpf()));
                documentJson.addProperty("roomNumber", document.getRoomNumber());
                documentJson.addProperty("roomType", document.getRoomType().getValue());
                documentJson.addProperty("checkIn", document.getCheckIn().toString());
                documentJson.addProperty("checkOut", document.getCheckOut().toString());
                documentJson.addProperty("nights", document.getNights());
                documentJson.addProperty("subtotal", document.getSubtotal().doubleValue());
                documentJson.addProperty("discount", document.getDiscount().doubleValue());
                documentJson.addProperty("total", document.getTotal().doubleValue());
                ctx.json(documentJson);
            } catch (NumberFormatException e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "Invalid reservation ID");
                ctx.status(400).json(error);
            }
        });
    }

    private static Object reservationToJson(Reservation res) {
        JsonObject json = new JsonObject();
        json.addProperty("id", res.getId());
        json.addProperty("roomNumber", res.getRoomNumber());
        json.addProperty("guestName", res.getGuestName());
        json.addProperty("guestEmail", res.getGuestEmail());
        json.addProperty("guestCpf", formatCpf(res.getGuestCpf()));
        json.addProperty("checkIn", res.getCheckIn().toString());
        json.addProperty("checkOut", res.getCheckOut().toString());
        json.addProperty("guests", res.getGuests());
        json.addProperty("ratePlan", res.getRatePlan().getValue());
        json.addProperty("nights", res.getNights());
        json.addProperty("subtotal", res.getSubtotal().doubleValue());
        json.addProperty("discount", res.getDiscount().doubleValue());
        json.addProperty("total", res.getTotal().doubleValue());
        json.addProperty("status", res.getStatus().getValue());
        json.addProperty("amountPaid", res.getAmountPaid().doubleValue());
        if (res.getRefund() != null) {
            json.addProperty("refund", res.getRefund().doubleValue());
        } else {
            json.add("refund", null);
        }
        return json;
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private static void handleError(io.javalin.http.Context ctx, String errorCode) {
        JsonObject error = new JsonObject();
        int status = 400;

        switch (errorCode) {
            case "ROOM_NOT_FOUND" -> {
                status = 404;
                error.addProperty("error", "ROOM_NOT_FOUND");
                error.addProperty("message", "Room not found");
            }
            case "RESERVATION_NOT_FOUND" -> {
                status = 404;
                error.addProperty("error", "RESERVATION_NOT_FOUND");
                error.addProperty("message", "Reservation not found");
            }
            case "ROOM_UNAVAILABLE" -> {
                status = 409;
                error.addProperty("error", "ROOM_UNAVAILABLE");
                error.addProperty("message", "Room is not available for the requested dates");
            }
            case "CAPACITY_EXCEEDED" -> {
                status = 422;
                error.addProperty("error", "CAPACITY_EXCEEDED");
                error.addProperty("message", "Number of guests exceeds room capacity");
            }
            case "OPERATION_NOT_ALLOWED" -> {
                status = 409;
                error.addProperty("error", "OPERATION_NOT_ALLOWED");
                error.addProperty("message", "This operation is not allowed for the current reservation status");
            }
            default -> {
                status = 400;
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", errorCode);
            }
        }

        ctx.status(status).json(error);
    }
}
