package com.pousada.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pousada.dao.RoomDAO;
import com.pousada.model.Reservation;
import com.pousada.model.Room;
import com.pousada.service.ReservationService;
import com.pousada.util.DocumentFormatter;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class ReservationHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReservationHandler.class);
    private static final ReservationService reservationService = new ReservationService();
    private static final RoomDAO roomDAO = new RoomDAO();
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void createReservation(Context ctx) {
        try {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();

            // Validate required fields
            if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
                !body.has("checkIn") || !body.has("checkOut") || !body.has("guests") || !body.has("ratePlan")) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "Missing required fields");
                ctx.status(400).json(error);
                return;
            }

            // Validate identification fields
            int identificationCount = 0;
            String guestCpf = null;
            String companyCnpj = null;
            String companyName = null;
            String guestPassport = null;
            String guestCountry = null;

            if (body.has("guestCpf")) {
                guestCpf = body.get("guestCpf").getAsString();
                identificationCount++;
            }
            if (body.has("companyCnpj")) {
                companyCnpj = body.get("companyCnpj").getAsString();
                identificationCount++;
            }
            if (body.has("companyName")) {
                companyName = body.get("companyName").getAsString();
            }
            if (body.has("guestPassport")) {
                guestPassport = body.get("guestPassport").getAsString();
                identificationCount++;
            }
            if (body.has("guestCountry")) {
                guestCountry = body.get("guestCountry").getAsString();
            }

            // Validate that exactly one identification method is provided
            if (identificationCount != 1) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "Exactly one identification method must be provided");
                ctx.status(400).json(error);
                return;
            }

            // Validate CNPJ requires companyName
            if (companyCnpj != null && companyName == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "companyName is required when companyCnpj is provided");
                ctx.status(400).json(error);
                return;
            }

            // Validate Passport requires Country
            if (guestPassport != null && guestCountry == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "guestCountry is required when guestPassport is provided");
                ctx.status(400).json(error);
                return;
            }

            int roomNumber = body.get("roomNumber").getAsInt();
            String guestName = body.get("guestName").getAsString();
            String guestEmail = body.get("guestEmail").getAsString();
            LocalDate checkIn = LocalDate.parse(body.get("checkIn").getAsString(), ISO_DATE);
            LocalDate checkOut = LocalDate.parse(body.get("checkOut").getAsString(), ISO_DATE);
            int guests = body.get("guests").getAsInt();
            String ratePlan = body.get("ratePlan").getAsString();

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail,
                guestCpf, companyCnpj, companyName,
                guestPassport, guestCountry,
                checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(toJson(reservation));
        } catch (DateTimeParseException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("message", "Invalid date format");
            ctx.status(400).json(error);
        } catch (IllegalArgumentException e) {
            String errorCode = e.getMessage();
            int status = 400;
            if ("ROOM_NOT_FOUND".equals(errorCode)) {
                status = 404;
            } else if ("ROOM_UNAVAILABLE".equals(errorCode)) {
                status = 409;
            } else if ("CAPACITY_EXCEEDED".equals(errorCode)) {
                status = 422;
            } else if ("INVALID_REQUEST".equals(errorCode)) {
                status = 400;
            }
            JsonObject error = new JsonObject();
            error.addProperty("error", errorCode);
            error.addProperty("message", errorCode);
            ctx.status(status).json(error);
        } catch (Exception e) {
            logger.error("Error creating reservation", e);
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("message", "Invalid request body");
            ctx.status(400).json(error);
        }
    }

    public static void getReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(toJson(reservation));
        } catch (NumberFormatException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("message", "Invalid reservation ID");
            ctx.status(400).json(error);
        } catch (IllegalArgumentException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            error.addProperty("message", "Reservation not found");
            ctx.status(404).json(error);
        }
    }

    public static void confirmReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();

            if (!body.has("amountPaid")) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "amountPaid is required");
                ctx.status(400).json(error);
                return;
            }

            BigDecimal amountPaid = body.get("amountPaid").getAsBigDecimal();
            Reservation reservation = reservationService.confirmReservation(id, amountPaid);
            ctx.json(toJson(reservation));
        } catch (NumberFormatException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("message", "Invalid reservation ID");
            ctx.status(400).json(error);
        } catch (IllegalArgumentException e) {
            String errorCode = e.getMessage();
            int status = "RESERVATION_NOT_FOUND".equals(errorCode) ? 404 : 409;
            JsonObject error = new JsonObject();
            error.addProperty("error", errorCode);
            error.addProperty("message", errorCode);
            ctx.status(status).json(error);
        } catch (Exception e) {
            logger.error("Error confirming reservation", e);
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("message", "Invalid request");
            ctx.status(400).json(error);
        }
    }

    public static void cancelReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toJson(reservation));
        } catch (NumberFormatException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("message", "Invalid reservation ID");
            ctx.status(400).json(error);
        } catch (IllegalArgumentException e) {
            String errorCode = e.getMessage();
            int status = "RESERVATION_NOT_FOUND".equals(errorCode) ? 404 : 409;
            JsonObject error = new JsonObject();
            error.addProperty("error", errorCode);
            error.addProperty("message", errorCode);
            ctx.status(status).json(error);
        }
    }

    public static void checkIn(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toJson(reservation));
        } catch (NumberFormatException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("message", "Invalid reservation ID");
            ctx.status(400).json(error);
        } catch (IllegalArgumentException e) {
            String errorCode = e.getMessage();
            int status = "RESERVATION_NOT_FOUND".equals(errorCode) ? 404 : 409;
            JsonObject error = new JsonObject();
            error.addProperty("error", errorCode);
            error.addProperty("message", errorCode);
            ctx.status(status).json(error);
        }
    }

    public static void checkOut(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            Optional<Room> roomOpt = roomDAO.findByNumber(reservation.getRoomNumber());

            if (roomOpt.isEmpty()) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INTERNAL_ERROR");
                error.addProperty("message", "Room not found");
                ctx.status(500).json(error);
                return;
            }

            Room room = roomOpt.get();
            JsonObject response = new JsonObject();
            response.addProperty("title", "Comprovante de reserva");
            response.addProperty("reservationId", reservation.getId());
            response.addProperty("guestName", reservation.getGuestName());
            response.addProperty("guestCpf", reservation.getGuestCpf());
            response.addProperty("companyCnpj", reservation.getCompanyCnpj());
            response.addProperty("companyName", reservation.getCompanyName());
            response.addProperty("guestPassport", reservation.getGuestPassport());
            response.addProperty("guestCountry", reservation.getGuestCountry());
            response.addProperty("roomNumber", reservation.getRoomNumber());
            response.addProperty("roomType", room.getType());
            response.addProperty("checkIn", DocumentFormatter.formatISODate(reservation.getCheckIn()));
            response.addProperty("checkOut", DocumentFormatter.formatISODate(reservation.getCheckOut()));
            response.addProperty("nights", reservation.getNights());
            response.addProperty("subtotal", reservation.getSubtotal());
            response.addProperty("discount", reservation.getDiscount());
            response.addProperty("total", reservation.getTotal());

            String document;
            if (reservation.getGuestCpf() != null) {
                document = DocumentFormatter.formatReceipt(reservation, room);
            } else if (reservation.getCompanyCnpj() != null) {
                document = DocumentFormatter.formatInvoice(reservation, room);
            } else {
                document = DocumentFormatter.formatBookingReceipt(reservation, room);
            }
            response.addProperty("text", document);

            ctx.json(response);
        } catch (NumberFormatException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("message", "Invalid reservation ID");
            ctx.status(400).json(error);
        } catch (IllegalArgumentException e) {
            String errorCode = e.getMessage();
            int status = "RESERVATION_NOT_FOUND".equals(errorCode) ? 404 : 409;
            JsonObject error = new JsonObject();
            error.addProperty("error", errorCode);
            error.addProperty("message", errorCode);
            ctx.status(status).json(error);
        }
    }

    public static void getDocument(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (!"CONFIRMED".equals(reservation.getStatus()) &&
                !"CHECKED_IN".equals(reservation.getStatus()) &&
                !"CHECKED_OUT".equals(reservation.getStatus())) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "OPERATION_NOT_ALLOWED");
                error.addProperty("message", "Document only available for paid reservations");
                ctx.status(409).json(error);
                return;
            }

            Optional<Room> roomOpt = roomDAO.findByNumber(reservation.getRoomNumber());
            if (roomOpt.isEmpty()) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INTERNAL_ERROR");
                error.addProperty("message", "Room not found");
                ctx.status(500).json(error);
                return;
            }

            Room room = roomOpt.get();
            JsonObject response = new JsonObject();
            response.addProperty("title", "Comprovante de reserva");
            response.addProperty("reservationId", reservation.getId());
            response.addProperty("guestName", reservation.getGuestName());
            response.addProperty("guestCpf", reservation.getGuestCpf());
            response.addProperty("companyCnpj", reservation.getCompanyCnpj());
            response.addProperty("companyName", reservation.getCompanyName());
            response.addProperty("guestPassport", reservation.getGuestPassport());
            response.addProperty("guestCountry", reservation.getGuestCountry());
            response.addProperty("roomNumber", reservation.getRoomNumber());
            response.addProperty("roomType", room.getType());
            response.addProperty("checkIn", DocumentFormatter.formatISODate(reservation.getCheckIn()));
            response.addProperty("checkOut", DocumentFormatter.formatISODate(reservation.getCheckOut()));
            response.addProperty("nights", reservation.getNights());
            response.addProperty("subtotal", reservation.getSubtotal());
            response.addProperty("discount", reservation.getDiscount());
            response.addProperty("total", reservation.getTotal());

            String document;
            if (reservation.getGuestCpf() != null) {
                document = DocumentFormatter.formatReceipt(reservation, room);
            } else if (reservation.getCompanyCnpj() != null) {
                document = DocumentFormatter.formatInvoice(reservation, room);
            } else {
                document = DocumentFormatter.formatBookingReceipt(reservation, room);
            }
            response.addProperty("text", document);

            ctx.json(response);
        } catch (NumberFormatException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("message", "Invalid reservation ID");
            ctx.status(400).json(error);
        } catch (IllegalArgumentException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            error.addProperty("message", "Reservation not found");
            ctx.status(404).json(error);
        }
    }

    private static JsonObject toJson(Reservation reservation) {
        JsonObject json = new JsonObject();
        json.addProperty("id", reservation.getId());
        json.addProperty("roomNumber", reservation.getRoomNumber());
        json.addProperty("guestName", reservation.getGuestName());
        json.addProperty("guestEmail", reservation.getGuestEmail());
        json.addProperty("guestCpf", reservation.getGuestCpf());
        json.addProperty("companyCnpj", reservation.getCompanyCnpj());
        json.addProperty("companyName", reservation.getCompanyName());
        json.addProperty("guestPassport", reservation.getGuestPassport());
        json.addProperty("guestCountry", reservation.getGuestCountry());
        json.addProperty("checkIn", DocumentFormatter.formatISODate(reservation.getCheckIn()));
        json.addProperty("checkOut", DocumentFormatter.formatISODate(reservation.getCheckOut()));
        json.addProperty("guests", reservation.getGuests());
        json.addProperty("ratePlan", reservation.getRatePlan());
        json.addProperty("nights", reservation.getNights());
        json.addProperty("subtotal", reservation.getSubtotal());
        json.addProperty("discount", reservation.getDiscount());
        json.addProperty("total", reservation.getTotal());
        json.addProperty("status", reservation.getStatus());
        json.addProperty("amountPaid", reservation.getAmountPaid());
        json.addProperty("refund", reservation.getRefund());
        return json;
    }
}
