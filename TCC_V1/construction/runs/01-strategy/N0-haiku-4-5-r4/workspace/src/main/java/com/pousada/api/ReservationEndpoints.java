package com.pousada.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pousada.entity.Reservation;
import com.pousada.service.MessageService;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationEndpoints {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void register(Javalin app) {
        app.post("/reservations", ctx -> {
            try {
                var body = ctx.bodyAsClass(ObjectNode.class);

                int roomNumber = getIntField(body, "roomNumber");
                String guestName = getStringField(body, "guestName");
                String guestEmail = getStringField(body, "guestEmail");
                String guestCpf = getStringField(body, "guestCpf");
                String checkInStr = getStringField(body, "checkIn");
                String checkOutStr = getStringField(body, "checkOut");
                int guests = getIntField(body, "guests");
                String ratePlan = getStringField(body, "ratePlan");
                String companyCode = body.has("companyCode") ? body.get("companyCode").asText() : null;

                if (guestName == null || guestEmail == null || guestCpf == null || ratePlan == null) {
                    throw new IllegalArgumentException("INVALID_REQUEST");
                }

                LocalDate checkIn = parseDate(checkInStr);
                LocalDate checkOut = parseDate(checkOutStr);

                var service = new ReservationService();
                var reservation = service.createReservation(roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan, companyCode);

                ctx.status(201).json(mapReservationResponse(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (SQLException e) {
                ctx.status(500).json(createError("DATABASE_ERROR", e.getMessage()));
            } catch (DateTimeParseException e) {
                ctx.status(400).json(createError("INVALID_REQUEST", "Invalid date format"));
            } catch (Exception e) {
                ctx.status(400).json(createError("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.get("/reservations/:id", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                var service = new ReservationService();
                var reservation = service.getReservation(id);

                if (reservation == null) {
                    ctx.status(404).json(createError("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else {
                    ctx.json(mapReservationResponse(reservation));
                }
            } catch (SQLException e) {
                ctx.status(500).json(createError("DATABASE_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/:id/confirmation", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                var body = ctx.bodyAsClass(ObjectNode.class);
                BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").asText());

                var service = new ReservationService();
                var reservation = service.confirmReservation(id, amountPaid);
                ctx.json(mapReservationResponse(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (SQLException e) {
                ctx.status(500).json(createError("DATABASE_ERROR", e.getMessage()));
            } catch (Exception e) {
                ctx.status(400).json(createError("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.post("/reservations/:id/cancellation", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                var service = new ReservationService();
                var reservation = service.cancelReservation(id);
                ctx.json(mapReservationResponse(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (SQLException e) {
                ctx.status(500).json(createError("DATABASE_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/:id/check-in", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                var service = new ReservationService();
                var reservation = service.checkIn(id);
                ctx.json(mapReservationResponse(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (SQLException e) {
                ctx.status(500).json(createError("DATABASE_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/:id/check-out", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                var service = new ReservationService();
                var reservation = service.checkOut(id);

                var messageService = new MessageService();
                String voucher = messageService.generateVoucher(reservation);

                ctx.json(createVoucherResponse(reservation, voucher));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (SQLException e) {
                ctx.status(500).json(createError("DATABASE_ERROR", e.getMessage()));
            }
        });

        app.get("/reservations/:id/document", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                var service = new ReservationService();
                var reservation = service.getReservation(id);

                if (reservation == null) {
                    ctx.status(404).json(createError("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (!reservation.status.equals("CONFIRMED") && !reservation.status.equals("CHECKED_IN") &&
                    !reservation.status.equals("CHECKED_OUT")) {
                    ctx.status(409).json(createError("OPERATION_NOT_ALLOWED",
                        "Document only available for paid reservations"));
                    return;
                }

                var messageService = new MessageService();
                String voucher = messageService.generateVoucher(reservation);
                ctx.json(createVoucherResponse(reservation, voucher));
            } catch (SQLException e) {
                ctx.status(500).json(createError("DATABASE_ERROR", e.getMessage()));
            }
        });
    }

    private static Object mapReservationResponse(Reservation res) {
        var obj = new java.util.LinkedHashMap<>();
        obj.put("id", res.id);
        obj.put("roomNumber", res.roomNumber);
        obj.put("roomType", res.roomType);
        obj.put("guestName", res.guestName);
        obj.put("guestEmail", res.guestEmail);
        obj.put("guestCpf", res.guestCpf);
        obj.put("checkIn", res.checkIn.toString());
        obj.put("checkOut", res.checkOut.toString());
        obj.put("guests", res.guests);
        obj.put("ratePlan", res.ratePlan);
        obj.put("nights", res.nights);
        obj.put("subtotal", Double.parseDouble(String.format("%.2f", res.subtotal)));
        obj.put("discount", Double.parseDouble(String.format("%.2f", res.discount)));
        obj.put("total", Double.parseDouble(String.format("%.2f", res.total)));
        obj.put("status", res.status);
        obj.put("amountPaid", Double.parseDouble(String.format("%.2f", res.amountPaid)));
        obj.put("refund", res.refund == null ? null : Double.parseDouble(String.format("%.2f", res.refund)));
        return obj;
    }

    private static Object createVoucherResponse(Reservation res, String voucher) {
        var obj = new java.util.LinkedHashMap<>();
        obj.put("title", "Comprovante de reserva");
        obj.put("reservationId", res.id);
        obj.put("guestName", res.guestName);
        obj.put("guestCpf", res.guestCpf);
        obj.put("roomNumber", res.roomNumber);
        obj.put("roomType", res.roomType);
        obj.put("checkIn", res.checkIn.toString());
        obj.put("checkOut", res.checkOut.toString());
        obj.put("nights", res.nights);
        obj.put("subtotal", Double.parseDouble(String.format("%.2f", res.subtotal)));
        obj.put("discount", Double.parseDouble(String.format("%.2f", res.discount)));
        obj.put("total", Double.parseDouble(String.format("%.2f", res.total)));
        return obj;
    }

    private static Object createError(String code, String msg) {
        return new Object() {
            public final String error = code;
            public final String message = msg;
        };
    }

    private static void handleError(io.javalin.http.Context ctx, String errorCode) {
        String message = switch (errorCode) {
            case "ROOM_NOT_FOUND" -> "Room not found";
            case "RESERVATION_NOT_FOUND" -> "Reservation not found";
            case "ROOM_UNAVAILABLE" -> "Room is unavailable for the selected dates";
            case "OPERATION_NOT_ALLOWED" -> "This operation is not allowed for the current reservation state";
            case "CAPACITY_EXCEEDED" -> "Too many guests for this room";
            case "INVALID_REQUEST" -> "Invalid request";
            default -> "An error occurred";
        };

        int status = switch (errorCode) {
            case "ROOM_NOT_FOUND", "RESERVATION_NOT_FOUND" -> 404;
            case "ROOM_UNAVAILABLE", "OPERATION_NOT_ALLOWED" -> 409;
            case "CAPACITY_EXCEEDED" -> 422;
            default -> 400;
        };

        ctx.status(status).json(createError(errorCode, message));
    }

    private static int getIntField(ObjectNode body, String field) throws IllegalArgumentException {
        if (!body.has(field)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
        return body.get(field).asInt();
    }

    private static String getStringField(ObjectNode body, String field) throws IllegalArgumentException {
        if (!body.has(field)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
        return body.get(field).asText();
    }

    private static LocalDate parseDate(String dateStr) throws DateTimeParseException {
        return LocalDate.parse(dateStr);
    }
}
