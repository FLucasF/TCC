package com.pousada.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.db.Receipt;
import com.pousada.db.Reservation;
import com.pousada.db.Room;
import com.pousada.service.*;
import com.pousada.util.DateFormatter;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class ReservationAPI {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void register(Javalin app) {
        app.post("/reservations", ctx -> createReservation(ctx));
        app.get("/reservations/{id}", ctx -> getReservation(ctx));
        app.post("/reservations/{id}/confirmation", ctx -> confirmReservation(ctx));
        app.post("/reservations/{id}/cancellation", ctx -> cancelReservation(ctx));
        app.post("/reservations/{id}/check-in", ctx -> checkIn(ctx));
        app.post("/reservations/{id}/check-out", ctx -> checkOut(ctx));
        app.get("/reservations/{id}/document", ctx -> getDocument(ctx));
    }

    private static void createReservation(io.javalin.http.Context ctx) {
        try {
            JsonNode body = mapper.readTree(ctx.body());

            Integer roomNumber = getIntField(body, "roomNumber");
            String guestName = getStringField(body, "guestName");
            String guestEmail = getStringField(body, "guestEmail");
            String guestCpf = getStringField(body, "guestCpf");
            String checkInStr = getStringField(body, "checkIn");
            String checkOutStr = getStringField(body, "checkOut");
            Integer guests = getIntField(body, "guests");
            String ratePlan = getStringField(body, "ratePlan");
            String companyCode = body.has("companyCode") ? body.get("companyCode").asText() : null;

            if (roomNumber == null || guestName == null || guestEmail == null || guestCpf == null ||
                checkInStr == null || checkOutStr == null || guests == null || ratePlan == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            LocalDate checkIn = DateFormatter.parseISO(checkInStr);
            LocalDate checkOut = DateFormatter.parseISO(checkOutStr);

            Room room = RoomService.getRoomByNumber(roomNumber);
            if (room == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                return;
            }

            Validator.ValidationError validationError = Validator.validateReservationRequest(
                roomNumber, checkIn, checkOut, guests, ratePlan, companyCode, room.capacity);

            if (validationError != null) {
                ctx.status("CAPACITY_EXCEEDED".equals(validationError.code) ? 422 : 400);
                ctx.json(new ErrorResponse(validationError.code, validationError.message));
                return;
            }

            if (!ReservationService.isRoomAvailable(roomNumber, checkIn, checkOut)) {
                ctx.status(409);
                ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for these dates"));
                return;
            }

            var pricingResult = RatePlanCalculator.calculate(
                ratePlan, roomNumber, room.nightlyRate, checkIn, checkOut, guests, companyCode);

            Reservation reservation = ReservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan, companyCode,
                pricingResult.subtotal, pricingResult.discount, pricingResult.total, pricingResult.nights);

            ctx.status(201);
            ctx.json(reservation);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request body"));
        }
    }

    private static void getReservation(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(reservation);
        } catch (NumberFormatException e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
        }
    }

    private static void confirmReservation(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (!"PENDING".equals(reservation.status)) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Reservation cannot be confirmed from this status"));
                return;
            }

            ReservationService.confirmReservation(id, reservation.total);

            Receipt receipt = ReceiptService.createReceipt(reservation);
            String receiptText = ReceiptService.formatReceipt(receipt);
            String emailBody = ReceiptService.createConfirmationEmail(reservation.guestName, receipt);

            MessageService.sendMessage("EMAIL", reservation.guestEmail, "Reserva confirmada - Pousada Maré Alta", emailBody);

            reservation = ReservationService.getReservationById(id);
            ctx.json(reservation);
        } catch (NumberFormatException e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
        }
    }

    private static void cancelReservation(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (!("PENDING".equals(reservation.status) || "CONFIRMED".equals(reservation.status))) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Reservation cannot be cancelled from this status"));
                return;
            }

            BigDecimal refundAmount = RatePlanCalculator.getRefundAmount(reservation.ratePlan, reservation.amountPaid);
            ReservationService.refundReservation(id, refundAmount);

            reservation = ReservationService.getReservationById(id);
            ctx.json(reservation);
        } catch (NumberFormatException e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
        }
    }

    private static void checkIn(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (!"CONFIRMED".equals(reservation.status)) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Only confirmed reservations can be checked in"));
                return;
            }

            ReservationService.updateReservationStatus(id, "CHECKED_IN");
            reservation = ReservationService.getReservationById(id);
            ctx.json(reservation);
        } catch (NumberFormatException e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
        }
    }

    private static void checkOut(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (!"CHECKED_IN".equals(reservation.status)) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Only checked in reservations can be checked out"));
                return;
            }

            ReservationService.updateReservationStatus(id, "CHECKED_OUT");

            Receipt receipt = ReceiptService.createReceipt(reservation);
            ctx.json(receipt);
        } catch (NumberFormatException e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
        }
    }

    private static void getDocument(io.javalin.http.Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationService.getReservationById(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (!("CONFIRMED".equals(reservation.status) || "CHECKED_IN".equals(reservation.status) || "CHECKED_OUT".equals(reservation.status))) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Document not found"));
                return;
            }

            Receipt receipt = ReceiptService.createReceipt(reservation);
            ctx.json(receipt);
        } catch (NumberFormatException e) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
        }
    }

    private static Integer getIntField(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        try {
            return node.get(field).asInt();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getStringField(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
