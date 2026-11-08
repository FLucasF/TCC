package com.pousadamareaita;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamareaita.model.Reservation;
import com.pousadamareaita.service.ReservationService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

public class ReservationHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReservationHandler.class);
    private static final ReservationService reservationService = new ReservationService();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void createReservation(Context ctx) {
        try {
            JsonNode body = objectMapper.readTree(ctx.body());

            if (!hasRequiredFields(body, "roomNumber", "guestName", "guestEmail", "guestCpf",
                                  "checkIn", "checkOut", "guests", "ratePlan")) {
                sendError(ctx, "INVALID_REQUEST", "Missing required fields", 400);
                return;
            }

            int roomNumber = body.get("roomNumber").asInt();
            String guestName = body.get("guestName").asText();
            String guestEmail = body.get("guestEmail").asText();
            String guestCpf = body.get("guestCpf").asText();
            LocalDate checkIn = LocalDate.parse(body.get("checkIn").asText(), dateFormatter);
            LocalDate checkOut = LocalDate.parse(body.get("checkOut").asText(), dateFormatter);
            int guests = body.get("guests").asInt();
            String ratePlan = body.get("ratePlan").asText();
            String companyCode = body.has("companyCode") ? body.get("companyCode").asText() : null;

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan, companyCode
            );

            ctx.status(201);
            ctx.json(reservationToJson(reservation));
        } catch (ReservationService.ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            logger.error("Error creating reservation", e);
            sendError(ctx, "INVALID_REQUEST", "Invalid request body", 400);
        }
    }

    public static void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> reservationOpt = reservationService.getReservation(id);

            if (reservationOpt.isEmpty()) {
                sendError(ctx, "RESERVATION_NOT_FOUND", "Reservation not found", 404);
                return;
            }

            ctx.json(reservationToJson(reservationOpt.get()));
        } catch (ReservationService.ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            logger.error("Error fetching reservation", e);
            sendError(ctx, "INVALID_REQUEST", "Invalid reservation ID", 400);
        }
    }

    public static void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            JsonNode body = objectMapper.readTree(ctx.body());

            if (!body.has("amountPaid")) {
                sendError(ctx, "INVALID_REQUEST", "Missing amountPaid field", 400);
                return;
            }

            BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").asText());
            Reservation reservation = reservationService.confirmReservation(id, amountPaid);

            ctx.json(reservationToJson(reservation));
        } catch (ReservationService.ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            logger.error("Error confirming reservation", e);
            sendError(ctx, "INVALID_REQUEST", "Invalid request", 400);
        }
    }

    public static void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);

            ctx.json(reservationToJson(reservation));
        } catch (ReservationService.ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            logger.error("Error cancelling reservation", e);
            sendError(ctx, "INVALID_REQUEST", "Invalid reservation ID", 400);
        }
    }

    public static void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);

            ctx.json(reservationToJson(reservation));
        } catch (ReservationService.ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            logger.error("Error checking in", e);
            sendError(ctx, "INVALID_REQUEST", "Invalid reservation ID", 400);
        }
    }

    public static void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);

            Map<String, Object> document = createDocument(reservation);
            ctx.json(document);
        } catch (ReservationService.ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            logger.error("Error checking out", e);
            sendError(ctx, "INVALID_REQUEST", "Invalid reservation ID", 400);
        }
    }

    public static void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> reservationOpt = reservationService.getReservation(id);

            if (reservationOpt.isEmpty()) {
                sendError(ctx, "RESERVATION_NOT_FOUND", "Reservation not found", 404);
                return;
            }

            Reservation reservation = reservationOpt.get();
            if (reservation.getStatus() == Reservation.Status.PENDING) {
                sendError(ctx, "OPERATION_NOT_ALLOWED", "Document only available for paid reservations", 409);
                return;
            }

            Map<String, Object> document = createDocument(reservation);
            ctx.json(document);
        } catch (ReservationService.ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            logger.error("Error fetching document", e);
            sendError(ctx, "INVALID_REQUEST", "Invalid reservation ID", 400);
        }
    }

    private static Map<String, Object> createDocument(Reservation reservation) {
        String roomType = switch (reservation.getRoomNumber()) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };

        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", reservation.getId());
        map.put("guestName", reservation.getGuestName());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("roomType", roomType);
        map.put("checkIn", reservation.getCheckIn());
        map.put("checkOut", reservation.getCheckOut());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        return map;
    }

    private static Map<String, Object> reservationToJson(Reservation reservation) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", reservation.getCheckIn());
        map.put("checkOut", reservation.getCheckOut());
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        map.put("status", reservation.getStatus());
        map.put("amountPaid", reservation.getAmountPaid());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private static boolean hasRequiredFields(JsonNode node, String... fields) {
        for (String field : fields) {
            if (!node.has(field)) {
                return false;
            }
        }
        return true;
    }

    private static void sendError(Context ctx, String code, String message, int statusCode) {
        ctx.status(statusCode);
        ctx.json(Map.of("error", code, "message", message));
    }
}
