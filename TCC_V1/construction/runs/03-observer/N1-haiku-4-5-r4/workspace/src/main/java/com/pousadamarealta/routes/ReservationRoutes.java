package com.pousadamarealta.routes;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamarealta.models.RatePlan;
import com.pousadamarealta.models.ReceiptResponse;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.utils.FormatterUtils;
import com.pousadamarealta.ErrorResponse;

import java.time.LocalDate;

public class ReservationRoutes {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void register(Javalin app) throws Exception {
        ReservationRepository reservationRepository = new ReservationRepository();
        RoomRepository roomRepository = new RoomRepository();
        MessageRepository messageRepository = new MessageRepository();
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            messageRepository
        );

        app.post("/reservations", ctx -> {
            try {
                JsonNode body = mapper.readTree(ctx.body());

                int roomNumber = body.get("roomNumber").asInt();
                String guestName = body.get("guestName").asText();
                String guestEmail = body.get("guestEmail").asText();
                String guestCpf = body.get("guestCpf").asText();
                LocalDate checkIn = LocalDate.parse(body.get("checkIn").asText());
                LocalDate checkOut = LocalDate.parse(body.get("checkOut").asText());
                int guests = body.get("guests").asInt();
                RatePlan ratePlan = RatePlan.valueOf(body.get("ratePlan").asText());

                var reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan
                );

                ctx.status(201);
                ctx.json(reservation);
            } catch (Exception e) {
                handleError(ctx, e);
            }
        });

        app.get("/reservations/:id", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.getReservation(id)
                    .orElseThrow(() -> new Exception("RESERVATION_NOT_FOUND"));
                ctx.json(reservation);
            } catch (Exception e) {
                handleError(ctx, e);
            }
        });

        app.post("/reservations/:id/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.confirmReservation(id);
                ctx.json(reservation);
            } catch (Exception e) {
                handleError(ctx, e);
            }
        });

        app.post("/reservations/:id/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.checkIn(id);
                ctx.json(reservation);
            } catch (Exception e) {
                handleError(ctx, e);
            }
        });

        app.post("/reservations/:id/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.checkOut(id);

                ReceiptResponse receipt = new ReceiptResponse(
                    reservation.id,
                    reservation.guestName,
                    reservation.guestCpf,
                    reservation.roomNumber,
                    reservation.roomType,
                    reservation.checkIn,
                    reservation.checkOut,
                    reservation.nights,
                    reservation.subtotal,
                    reservation.discount,
                    reservation.total
                );

                ctx.json(receipt);
            } catch (Exception e) {
                handleError(ctx, e);
            }
        });

        app.get("/reservations/:id/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.getReservation(id)
                    .orElseThrow(() -> new Exception("RESERVATION_NOT_FOUND"));

                ReceiptResponse receipt = new ReceiptResponse(
                    reservation.id,
                    reservation.guestName,
                    reservation.guestCpf,
                    reservation.roomNumber,
                    reservation.roomType,
                    reservation.checkIn,
                    reservation.checkOut,
                    reservation.nights,
                    reservation.subtotal,
                    reservation.discount,
                    reservation.total
                );

                ctx.json(receipt);
            } catch (Exception e) {
                handleError(ctx, e);
            }
        });

        app.post("/reservations/:id/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.cancelReservation(id);
                ctx.json(reservation);
            } catch (Exception e) {
                handleError(ctx, e);
            }
        });
    }

    private static void handleError(io.javalin.http.Context ctx, Exception e) {
        String message = e.getMessage();

        if (message.equals("ROOM_NOT_FOUND")) {
            ctx.status(404);
            ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } else if (message.equals("RESERVATION_NOT_FOUND")) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } else if (message.equals("ROOM_UNAVAILABLE")) {
            ctx.status(409);
            ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the requested dates"));
        } else if (message.equals("CAPACITY_EXCEEDED")) {
            ctx.status(422);
            ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        } else if (message.equals("OPERATION_NOT_ALLOWED")) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for this reservation status"));
        } else if (message.equals("INVALID_REQUEST")) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request data"));
        } else {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        }
    }
}
