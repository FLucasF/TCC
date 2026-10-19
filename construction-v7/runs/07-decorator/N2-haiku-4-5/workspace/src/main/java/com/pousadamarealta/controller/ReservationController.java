package com.pousadamarealta.controller;

import com.pousadamarealta.api.*;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.service.*;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomService roomService;
    private final MessageService messageService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReservationController(
        ReservationService reservationService,
        RoomService roomService,
        MessageService messageService
    ) {
        this.reservationService = reservationService;
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest body = ctx.bodyAsClass(CreateReservationRequest.class);

            if (body.roomNumber == 0 || body.guestName == null || body.guestEmail == null ||
                body.guestCpf == null || body.checkIn == null || body.checkOut == null ||
                body.ratePlan == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            int roomNumber = body.roomNumber;
            String guestName = body.guestName;
            String guestEmail = body.guestEmail;
            String guestCpf = body.guestCpf;
            String checkInStr = body.checkIn;
            String checkOutStr = body.checkOut;
            int guests = body.guests;
            String ratePlanStr = body.ratePlan;

            LocalDate checkIn = LocalDate.parse(checkInStr, DATE_FORMATTER);
            LocalDate checkOut = LocalDate.parse(checkOutStr, DATE_FORMATTER);

            RatePlan ratePlan = RatePlan.valueOf(ratePlanStr);

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201);
            ctx.json(reservationToResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid number format"));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(reservationToResponse(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            ConfirmReservationRequest body = ctx.bodyAsClass(ConfirmReservationRequest.class);

            if (body.amountPaid == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing amountPaid field"));
                return;
            }

            BigDecimal amountPaid = body.amountPaid;
            reservationService.confirmReservation(id, amountPaid);

            Reservation reservation = reservationService.getReservation(id);
            Room room = roomService.getRoom(reservation.getRoomNumber());

            String receipt = ReceiptGenerator.generate(reservation, room);
            String emailBody = "Olá, " + reservation.getGuestName() + "!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                receipt + "\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!";

            messageService.sendMessage(
                reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta",
                emailBody
            );

            ctx.json(reservationToResponse(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            reservationService.cancelReservation(id);

            Reservation reservation = reservationService.getReservation(id);
            ctx.json(reservationToResponse(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            reservationService.checkIn(id);

            Reservation reservation = reservationService.getReservation(id);
            ctx.json(reservationToResponse(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            reservationService.checkOut(id);

            Reservation reservation = reservationService.getReservation(id);
            Room room = roomService.getRoom(reservation.getRoomNumber());

            String receipt = ReceiptGenerator.generate(reservation, room);
            ReceiptResponse receiptResponse = new ReceiptResponse(
                "Comprovante de reserva",
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                room.getType().getCode(),
                reservation.getCheckIn().toString(),
                reservation.getCheckOut().toString(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
            );

            ctx.json(receiptResponse);
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() == ReservationStatus.PENDING) {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot get document for pending reservation"));
                return;
            }

            Room room = roomService.getRoom(reservation.getRoomNumber());

            ReceiptResponse receiptResponse = new ReceiptResponse(
                "Comprovante de reserva",
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                room.getType().getCode(),
                reservation.getCheckIn().toString(),
                reservation.getCheckOut().toString(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
            );

            ctx.json(receiptResponse);
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    private ReservationResponse reservationToResponse(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn().toString(),
            reservation.getCheckOut().toString(),
            reservation.getGuests(),
            reservation.getRatePlan().getCode(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            reservation.getStatus().getCode(),
            reservation.getAmountPaid(),
            reservation.getRefund()
        );
    }

    private void handleException(Context ctx, Exception e) {
        String message = e.getMessage();
        if ("ROOM_NOT_FOUND".equals(message)) {
            ctx.status(404);
            ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } else if ("RESERVATION_NOT_FOUND".equals(message)) {
            ctx.status(404);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } else if ("ROOM_UNAVAILABLE".equals(message)) {
            ctx.status(409);
            ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the requested dates"));
        } else if ("CAPACITY_EXCEEDED".equals(message)) {
            ctx.status(422);
            ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        } else if ("OPERATION_NOT_ALLOWED".equals(message)) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "This operation is not allowed in the current reservation state"));
        } else if ("INVALID_REQUEST".equals(message)) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        } else {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }
}
