package com.marealta.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.marealta.db.MessageRepository;
import com.marealta.db.RoomRepository;
import com.marealta.model.Reservation;
import com.marealta.model.Room;
import com.marealta.service.EmailService;
import com.marealta.service.ReceiptService;
import com.marealta.service.ReservationService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ReservationsHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReservationsHandler.class);
    private final ReservationService reservationService;
    private final EmailService emailService;
    private final ReceiptService receiptService;
    private final RoomRepository roomRepository;
    private final Gson gson;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReservationsHandler(ReservationService reservationService, EmailService emailService,
                              ReceiptService receiptService, RoomRepository roomRepository, Gson gson) {
        this.reservationService = reservationService;
        this.emailService = emailService;
        this.receiptService = receiptService;
        this.roomRepository = roomRepository;
        this.gson = gson;
    }

    public void createReservation(Context ctx) {
        try {
            JsonObject body = ctx.bodyAsClass(JsonObject.class);

            if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
                !body.has("guestCpf") || !body.has("checkIn") || !body.has("checkOut") ||
                !body.has("guests") || !body.has("ratePlan")) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            int roomNumber = body.get("roomNumber").getAsInt();
            String guestName = body.get("guestName").getAsString();
            String guestEmail = body.get("guestEmail").getAsString();
            String guestCpf = body.get("guestCpf").getAsString();
            LocalDate checkIn = LocalDate.parse(body.get("checkIn").getAsString(), DATE_FORMAT);
            LocalDate checkOut = LocalDate.parse(body.get("checkOut").getAsString(), DATE_FORMAT);
            int guests = body.get("guests").getAsInt();
            Reservation.RatePlan ratePlan = Reservation.RatePlan.valueOf(body.get("ratePlan").getAsString());

            Reservation reservation = reservationService.createReservation(roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan);

            ctx.status(201).json(toResponseDto(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating reservation", e);
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request format"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(toResponseDto(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            logger.error("Error getting reservation", e);
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            JsonObject body = ctx.bodyAsClass(JsonObject.class);

            if (!body.has("amountPaid")) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing amountPaid"));
                return;
            }

            BigDecimal amountPaid = body.get("amountPaid").getAsBigDecimal();
            Reservation reservation = reservationService.confirm(id, amountPaid);

            emailService.sendConfirmationEmail(reservation);

            ctx.json(toResponseDto(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            logger.error("Error confirming reservation", e);
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void checkInReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toResponseDto(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            logger.error("Error checking in reservation", e);
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void checkOutReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.checkOut(id);

            String receipt = receiptService.generateReceipt(reservation);
            JsonObject response = new JsonObject();
            response.addProperty("title", "Comprovante de reserva");
            response.addProperty("reservationId", reservation.getId());
            response.addProperty("guestName", reservation.getGuestName());
            response.addProperty("guestCpf", reservation.getGuestCpf());
            response.addProperty("roomNumber", reservation.getRoomNumber());
            response.addProperty("roomType", getRoomType(reservation.getRoomNumber()));
            response.addProperty("checkIn", reservation.getCheckIn().format(DATE_FORMAT));
            response.addProperty("checkOut", reservation.getCheckOut().format(DATE_FORMAT));
            response.addProperty("nights", reservation.getNights());
            response.addProperty("subtotal", reservation.getSubtotal());
            response.addProperty("discount", reservation.getDiscount());
            response.addProperty("total", reservation.getTotal());

            ctx.json(response);
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            logger.error("Error checking out reservation", e);
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.cancel(id);
            ctx.json(toResponseDto(reservation));
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            logger.error("Error canceling reservation", e);
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getStatus() != Reservation.Status.CONFIRMED &&
                reservation.getStatus() != Reservation.Status.CHECKED_IN &&
                reservation.getStatus() != Reservation.Status.CHECKED_OUT) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document not available"));
                return;
            }

            String receipt = receiptService.generateReceipt(reservation);
            JsonObject response = new JsonObject();
            response.addProperty("title", "Comprovante de reserva");
            response.addProperty("reservationId", reservation.getId());
            response.addProperty("guestName", reservation.getGuestName());
            response.addProperty("guestCpf", reservation.getGuestCpf());
            response.addProperty("roomNumber", reservation.getRoomNumber());
            response.addProperty("roomType", getRoomType(reservation.getRoomNumber()));
            response.addProperty("checkIn", reservation.getCheckIn().format(DATE_FORMAT));
            response.addProperty("checkOut", reservation.getCheckOut().format(DATE_FORMAT));
            response.addProperty("nights", reservation.getNights());
            response.addProperty("subtotal", reservation.getSubtotal());
            response.addProperty("discount", reservation.getDiscount());
            response.addProperty("total", reservation.getTotal());

            ctx.json(response);
        } catch (IllegalArgumentException e) {
            handleError(ctx, e.getMessage());
        } catch (Exception e) {
            logger.error("Error getting document", e);
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    private Object toResponseDto(Reservation reservation) {
        JsonObject response = new JsonObject();
        response.addProperty("id", reservation.getId());
        response.addProperty("roomNumber", reservation.getRoomNumber());
        response.addProperty("guestName", reservation.getGuestName());
        response.addProperty("guestEmail", reservation.getGuestEmail());
        response.addProperty("guestCpf", reservation.getGuestCpf());
        response.addProperty("checkIn", reservation.getCheckIn().format(DATE_FORMAT));
        response.addProperty("checkOut", reservation.getCheckOut().format(DATE_FORMAT));
        response.addProperty("guests", reservation.getGuests());
        response.addProperty("ratePlan", reservation.getRatePlan().toString());
        response.addProperty("nights", reservation.getNights());
        response.addProperty("subtotal", reservation.getSubtotal());
        response.addProperty("discount", reservation.getDiscount());
        response.addProperty("total", reservation.getTotal());
        response.addProperty("status", reservation.getStatus().toString());
        response.addProperty("amountPaid", reservation.getAmountPaid());
        response.addProperty("refund", reservation.getRefund());
        return response;
    }

    private String getRoomType(int roomNumber) {
        Optional<Room> room = roomRepository.findByNumber(roomNumber);
        return room.map(r -> r.type().toString()).orElse("UNKNOWN");
    }

    private void handleError(Context ctx, String errorCode) {
        switch (errorCode) {
            case "ROOM_NOT_FOUND":
                ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                break;
            case "RESERVATION_NOT_FOUND":
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                break;
            case "ROOM_UNAVAILABLE":
                ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room not available for those dates"));
                break;
            case "CAPACITY_EXCEEDED":
                ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Too many guests for this room"));
                break;
            case "OPERATION_NOT_ALLOWED":
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for this reservation status"));
                break;
            case "INVALID_REQUEST":
            default:
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
                break;
        }
    }

    static class ErrorResponse {
        String error;
        String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
