package com.pousada.marealta.routes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.marealta.exception.CapacityExceededException;
import com.pousada.marealta.exception.RoomUnavailableException;
import com.pousada.marealta.service.ReservationService;
import com.pousada.marealta.model.Reservation;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationRoutes {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void createReservation(Context ctx) {
        try {
            var body = mapper.readTree(ctx.body());

            if (!hasRequiredFields(body, "roomNumber", "guestName", "guestEmail", "guestCpf",
                    "checkIn", "checkOut", "guests", "ratePlan")) {
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            int roomNumber = body.get("roomNumber").asInt();
            String guestName = body.get("guestName").asText();
            String guestEmail = body.get("guestEmail").asText();
            String guestCpf = body.get("guestCpf").asText();
            String checkInStr = body.get("checkIn").asText();
            String checkOutStr = body.get("checkOut").asText();
            int guests = body.get("guests").asInt();
            String ratePlan = body.get("ratePlan").asText();

            if (guests < 1) {
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", "Guests must be at least 1"));
                return;
            }

            LocalDate checkIn, checkOut;
            try {
                checkIn = LocalDate.parse(checkInStr);
                checkOut = LocalDate.parse(checkOutStr);
            } catch (DateTimeParseException e) {
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            if (!checkIn.isBefore(checkOut)) {
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", "Check-in must be before check-out"));
                return;
            }

            try {
                var reservation = ReservationService.createReservation(roomNumber, guestName, guestEmail,
                        guestCpf, checkIn, checkOut, guests, ratePlan);

                if (reservation == null) {
                    ctx.status(404);
                    ctx.json(errorResponse("ROOM_NOT_FOUND", "Room not found"));
                    return;
                }

                ctx.status(201);
                ctx.json(serializeReservation(reservation));
            } catch (CapacityExceededException e) {
                ctx.status(422);
                ctx.json(errorResponse("CAPACITY_EXCEEDED", "Room capacity exceeded"));
            } catch (RoomUnavailableException e) {
                ctx.status(409);
                ctx.json(errorResponse("ROOM_UNAVAILABLE", "Room is unavailable for these dates"));
            } catch (IllegalArgumentException e) {
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", e.getMessage()));
            }
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(errorResponse("INVALID_REQUEST", "Invalid JSON"));
        }
    }

    public static void getReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            var reservation = ReservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(serializeReservation(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(errorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(errorResponse("INTERNAL_ERROR", "Database error"));
        }
    }

    public static void confirmReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));

            var body = mapper.readTree(ctx.body());
            if (!body.has("amountPaid")) {
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", "amountPaid is required"));
                return;
            }

            BigDecimal amountPaid = body.get("amountPaid").decimalValue();

            var reservation = ReservationService.confirmReservation(id, amountPaid);

            if (reservation == null) {
                ctx.status(409);
                ctx.json(errorResponse("OPERATION_NOT_ALLOWED", "Cannot confirm this reservation"));
                return;
            }

            ctx.json(serializeReservation(reservation));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(errorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public static void cancelReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            var reservation = ReservationService.cancelReservation(id);

            if (reservation == null) {
                ctx.status(409);
                ctx.json(errorResponse("OPERATION_NOT_ALLOWED", "Cannot cancel this reservation"));
                return;
            }

            ctx.json(serializeReservation(reservation));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(errorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public static void checkIn(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            var reservation = ReservationService.checkIn(id);

            if (reservation == null) {
                ctx.status(409);
                ctx.json(errorResponse("OPERATION_NOT_ALLOWED", "Cannot check in this reservation"));
                return;
            }

            ctx.json(serializeReservation(reservation));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(errorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public static void checkOut(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            var reservation = ReservationService.checkOut(id);

            if (reservation == null) {
                ctx.status(409);
                ctx.json(errorResponse("OPERATION_NOT_ALLOWED", "Cannot check out this reservation"));
                return;
            }

            var comprovante = ReservationService.generateComprovante(reservation);
            var document = createDocument(reservation, comprovante);

            ctx.json(document);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(errorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public static void getDocument(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            var reservation = ReservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (!reservation.status.equals("CONFIRMED") && !reservation.status.equals("CHECKED_IN")
                    && !reservation.status.equals("CHECKED_OUT")) {
                ctx.status(409);
                ctx.json(errorResponse("OPERATION_NOT_ALLOWED", "Document not available"));
                return;
            }

            var comprovante = ReservationService.generateComprovante(reservation);
            var document = createDocument(reservation, comprovante);

            ctx.json(document);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(errorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private static Map<String, Object> createDocument(Reservation reservation, String comprovante) {
        Map<String, Object> document = new HashMap<>();
        document.put("title", "Comprovante de reserva");
        document.put("reservationId", reservation.id);
        document.put("guestName", reservation.guestName);
        document.put("guestCpf", reservation.guestCpf);
        document.put("roomNumber", reservation.roomNumber);
        document.put("roomType", getRoomType(reservation.roomNumber));
        document.put("checkIn", reservation.checkIn.toString());
        document.put("checkOut", reservation.checkOut.toString());
        document.put("nights", reservation.nights);
        document.put("subtotal", reservation.subtotal);
        document.put("discount", reservation.discount);
        document.put("total", reservation.total);
        return document;
    }

    private static String getRoomType(int roomNumber) {
        try {
            var room = com.pousada.marealta.service.RoomService.getRoomById(roomNumber);
            return room != null ? room.type : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private static Map<String, Object> serializeReservation(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.id);
        map.put("roomNumber", r.roomNumber);
        map.put("guestName", r.guestName);
        map.put("guestEmail", r.guestEmail);
        map.put("guestCpf", r.guestCpf);
        map.put("checkIn", r.checkIn.toString());
        map.put("checkOut", r.checkOut.toString());
        map.put("guests", r.guests);
        map.put("ratePlan", r.ratePlan);
        map.put("nights", r.nights);
        map.put("subtotal", r.subtotal);
        map.put("discount", r.discount);
        map.put("total", r.total);
        map.put("status", r.status);
        map.put("amountPaid", r.amountPaid);
        map.put("refund", r.refund);
        return map;
    }

    private static boolean hasRequiredFields(JsonNode node, String... fields) {
        for (String field : fields) {
            if (!node.has(field) || node.get(field).isNull()) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Object> errorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        response.put("message", message);
        return response;
    }
}
