package com.posadamarealta.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.posadamarealta.model.Document;
import com.posadamarealta.model.Reservation;
import com.posadamarealta.model.Room;
import com.posadamarealta.repository.MessageRepository;
import com.posadamarealta.repository.ReservationRepository;
import com.posadamarealta.repository.RoomRepository;
import com.posadamarealta.service.DocumentService;
import com.posadamarealta.service.ReservationService;
import com.posadamarealta.service.UtilService;
import io.javalin.http.Context;
import javax.sql.DataSource;
import java.time.LocalDate;

public class ReservationHandler {
    private final ReservationService reservationService;
    private final RoomRepository roomRepo;
    private final ReservationRepository reservationRepo;
    private final MessageRepository messageRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReservationHandler(DataSource ds) {
        this.reservationRepo = new ReservationRepository(ds);
        this.roomRepo = new RoomRepository(ds);
        this.messageRepo = new MessageRepository(ds);
        this.reservationService = new ReservationService(reservationRepo, roomRepo, messageRepo);
    }

    public void create(Context ctx) {
        try {
            JsonNode body = mapper.readTree(ctx.body());

            if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
                !body.has("checkIn") || !body.has("checkOut") || !body.has("guests") || !body.has("ratePlan")) {
                sendError(ctx, 400, "INVALID_REQUEST", "Missing required fields");
                return;
            }

            int roomNumber = body.get("roomNumber").asInt();
            String guestName = body.get("guestName").asText();
            String guestEmail = body.get("guestEmail").asText();
            String guestCpf = body.has("guestCpf") ? body.get("guestCpf").asText() : null;
            String companyCnpj = body.has("companyCnpj") ? body.get("companyCnpj").asText() : null;
            String companyName = body.has("companyName") ? body.get("companyName").asText() : null;
            String guestPassport = body.has("guestPassport") ? body.get("guestPassport").asText() : null;
            String guestCountry = body.has("guestCountry") ? body.get("guestCountry").asText() : null;
            String checkInStr = body.get("checkIn").asText();
            String checkOutStr = body.get("checkOut").asText();
            int guests = body.get("guests").asInt();
            String ratePlan = body.get("ratePlan").asText();

            LocalDate checkIn = UtilService.parseDate(checkInStr);
            LocalDate checkOut = UtilService.parseDate(checkOutStr);

            Reservation reservation = reservationService.createReservation(roomNumber, guestName, guestEmail,
                guestCpf, companyCnpj, companyName, guestPassport, guestCountry,
                checkIn, checkOut, guests, ratePlan);

            ctx.status(201);
            ctx.json(reservation);
        } catch (IllegalArgumentException e) {
            String errorCode = e.getMessage();
            if ("ROOM_NOT_FOUND".equals(errorCode)) {
                sendError(ctx, 404, "ROOM_NOT_FOUND", "Room not found");
            } else if ("ROOM_UNAVAILABLE".equals(errorCode)) {
                sendError(ctx, 409, "ROOM_UNAVAILABLE", "Room is not available for the requested dates");
            } else if ("CAPACITY_EXCEEDED".equals(errorCode)) {
                sendError(ctx, 422, "CAPACITY_EXCEEDED", "Number of guests exceeds room capacity");
            } else {
                sendError(ctx, 400, "INVALID_REQUEST", "Invalid request");
            }
        } catch (Exception e) {
            sendError(ctx, 400, "INVALID_REQUEST", "Invalid request");
        }
    }

    public void get(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(reservation);
        } catch (IllegalArgumentException e) {
            sendError(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
        } catch (Exception e) {
            sendError(ctx, 400, "INVALID_REQUEST", "Invalid request");
        }
    }

    public void confirm(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(reservation);
        } catch (IllegalArgumentException e) {
            String errorCode = e.getMessage();
            if ("RESERVATION_NOT_FOUND".equals(errorCode)) {
                sendError(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
            } else if ("OPERATION_NOT_ALLOWED".equals(errorCode)) {
                sendError(ctx, 409, "OPERATION_NOT_ALLOWED", "Cannot confirm a reservation in this status");
            } else {
                sendError(ctx, 400, "INVALID_REQUEST", "Invalid request");
            }
        }
    }

    public void cancel(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(reservation);
        } catch (IllegalArgumentException e) {
            String errorCode = e.getMessage();
            if ("RESERVATION_NOT_FOUND".equals(errorCode)) {
                sendError(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
            } else if ("OPERATION_NOT_ALLOWED".equals(errorCode)) {
                sendError(ctx, 409, "OPERATION_NOT_ALLOWED", "Cannot cancel a reservation in this status");
            } else {
                sendError(ctx, 400, "INVALID_REQUEST", "Invalid request");
            }
        }
    }

    public void checkIn(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(reservation);
        } catch (IllegalArgumentException e) {
            String errorCode = e.getMessage();
            if ("RESERVATION_NOT_FOUND".equals(errorCode)) {
                sendError(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
            } else if ("OPERATION_NOT_ALLOWED".equals(errorCode)) {
                sendError(ctx, 409, "OPERATION_NOT_ALLOWED", "Cannot check in a reservation in this status");
            } else {
                sendError(ctx, 400, "INVALID_REQUEST", "Invalid request");
            }
        }
    }

    public void checkOut(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            Room room = roomRepo.findByNumber(reservation.roomNumber)
                .orElseThrow(() -> new IllegalArgumentException("ROOM_NOT_FOUND"));
            Document document = DocumentService.generate(reservation, room.type);
            ctx.json(document);
        } catch (IllegalArgumentException e) {
            String errorCode = e.getMessage();
            if ("RESERVATION_NOT_FOUND".equals(errorCode)) {
                sendError(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
            } else if ("OPERATION_NOT_ALLOWED".equals(errorCode)) {
                sendError(ctx, 409, "OPERATION_NOT_ALLOWED", "Cannot check out a reservation in this status");
            } else {
                sendError(ctx, 400, "INVALID_REQUEST", "Invalid request");
            }
        }
    }

    public void getDocument(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (!("CONFIRMED".equals(reservation.status) || "CHECKED_IN".equals(reservation.status) || "CHECKED_OUT".equals(reservation.status))) {
                sendError(ctx, 409, "OPERATION_NOT_ALLOWED", "Document is only available for paid reservations");
                return;
            }

            Room room = roomRepo.findByNumber(reservation.roomNumber)
                .orElseThrow(() -> new IllegalArgumentException("ROOM_NOT_FOUND"));
            Document document = DocumentService.generate(reservation, room.type);
            ctx.json(document);
        } catch (IllegalArgumentException e) {
            String errorCode = e.getMessage();
            if ("RESERVATION_NOT_FOUND".equals(errorCode)) {
                sendError(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
            } else {
                sendError(ctx, 400, "INVALID_REQUEST", "Invalid request");
            }
        }
    }

    private void sendError(Context ctx, int status, String code, String message) {
        ctx.status(status);
        ctx.json(new java.util.HashMap<String, String>() {{
            put("error", code);
            put("message", message);
        }});
    }
}
