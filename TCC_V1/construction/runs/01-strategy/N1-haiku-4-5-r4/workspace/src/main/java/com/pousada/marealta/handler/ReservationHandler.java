package com.pousada.marealta.handler;

import com.google.gson.JsonObject;
import com.pousada.marealta.model.RatePlan;
import com.pousada.marealta.model.ReceiptDto;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.ReservationDto;
import com.pousada.marealta.persistence.RoomDao;
import com.pousada.marealta.service.MessageService;
import com.pousada.marealta.service.ReservationService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationHandler {
    private final ReservationService reservationService;
    private final MessageService messageService;

    public ReservationHandler(ReservationService reservationService, MessageService messageService) {
        this.reservationService = reservationService;
        this.messageService = messageService;
    }

    public void createReservation(Context ctx) {
        try {
            JsonObject body = ctx.bodyAsClass(JsonObject.class);

            if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
                !body.has("guestCpf") || !body.has("checkIn") || !body.has("checkOut") ||
                !body.has("guests") || !body.has("ratePlan")) {
                ErrorHandler.handle400(ctx, "INVALID_REQUEST", "Missing required fields");
                return;
            }

            int roomNumber = body.get("roomNumber").getAsInt();
            String guestName = body.get("guestName").getAsString();
            String guestEmail = body.get("guestEmail").getAsString();
            String guestCpf = body.get("guestCpf").getAsString();
            String checkInStr = body.get("checkIn").getAsString();
            String checkOutStr = body.get("checkOut").getAsString();
            int guests = body.get("guests").getAsInt();
            String ratePlanStr = body.get("ratePlan").getAsString();
            String companyCode = body.has("companyCode") ? body.get("companyCode").getAsString() : null;

            try {
                LocalDate checkIn = LocalDate.parse(checkInStr);
                LocalDate checkOut = LocalDate.parse(checkOutStr);

                RatePlan ratePlan = RatePlan.valueOf(ratePlanStr);

                Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan, companyCode
                );

                ctx.status(201).json(reservationToJson(reservation));
            } catch (IllegalArgumentException e) {
                String msg = e.getMessage();
                if ("ROOM_NOT_FOUND".equals(msg)) {
                    ErrorHandler.handle404(ctx, "ROOM_NOT_FOUND", "Room not found");
                } else if ("ROOM_UNAVAILABLE".equals(msg)) {
                    ErrorHandler.handle409(ctx, "ROOM_UNAVAILABLE", "Room is not available for the requested dates");
                } else if ("CAPACITY_EXCEEDED".equals(msg)) {
                    ErrorHandler.handle422(ctx, "CAPACITY_EXCEEDED", "Number of guests exceeds room capacity");
                } else if ("INVALID_REQUEST".equals(msg)) {
                    ErrorHandler.handle400(ctx, "INVALID_REQUEST", "Invalid rate plan or dates");
                } else {
                    ErrorHandler.handle400(ctx, "INVALID_REQUEST", msg);
                }
            } catch (DateTimeParseException e) {
                ErrorHandler.handle400(ctx, "INVALID_REQUEST", "Invalid date format");
            }
        } catch (Exception e) {
            ErrorHandler.handleException(ctx, e);
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(reservationToJson(reservation));
        } catch (IllegalArgumentException e) {
            if ("RESERVATION_NOT_FOUND".equals(e.getMessage())) {
                ErrorHandler.handle404(ctx, "RESERVATION_NOT_FOUND", "Reservation not found");
            } else {
                ErrorHandler.handle400(ctx, "INVALID_REQUEST", e.getMessage());
            }
        } catch (Exception e) {
            ErrorHandler.handleException(ctx, e);
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            BigDecimal amountPaid = reservation.getTotal();

            try {
                JsonObject body = ctx.bodyAsClass(JsonObject.class);
                if (body != null && body.has("amountPaid")) {
                    amountPaid = new BigDecimal(body.get("amountPaid").getAsString());
                }
            } catch (Exception e) {
                // Body is optional, use total as default
            }

            Reservation confirmedReservation = reservationService.confirmReservation(id, amountPaid);
            ctx.json(reservationToJson(confirmedReservation));
        } catch (IllegalArgumentException e) {
            if ("RESERVATION_NOT_FOUND".equals(e.getMessage())) {
                ErrorHandler.handle404(ctx, "RESERVATION_NOT_FOUND", "Reservation not found");
            } else if ("OPERATION_NOT_ALLOWED".equals(e.getMessage())) {
                ErrorHandler.handle409(ctx, "OPERATION_NOT_ALLOWED", "Cannot confirm reservation in current status");
            } else {
                ErrorHandler.handle400(ctx, "INVALID_REQUEST", e.getMessage());
            }
        } catch (Exception e) {
            ErrorHandler.handleException(ctx, e);
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(reservationToJson(reservation));
        } catch (IllegalArgumentException e) {
            if ("RESERVATION_NOT_FOUND".equals(e.getMessage())) {
                ErrorHandler.handle404(ctx, "RESERVATION_NOT_FOUND", "Reservation not found");
            } else if ("OPERATION_NOT_ALLOWED".equals(e.getMessage())) {
                ErrorHandler.handle409(ctx, "OPERATION_NOT_ALLOWED", "Cannot check in reservation in current status");
            } else {
                ErrorHandler.handle400(ctx, "INVALID_REQUEST", e.getMessage());
            }
        } catch (Exception e) {
            ErrorHandler.handleException(ctx, e);
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            RoomDao roomDao = (RoomDao) ctx.attribute("roomDao");
            String roomType = roomDao.findByNumber(reservation.getRoomNumber()).getType().name();

            ReceiptDto receipt = new ReceiptDto(
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                roomType,
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
            );

            ctx.json(receipt);
        } catch (IllegalArgumentException e) {
            if ("RESERVATION_NOT_FOUND".equals(e.getMessage())) {
                ErrorHandler.handle404(ctx, "RESERVATION_NOT_FOUND", "Reservation not found");
            } else if ("OPERATION_NOT_ALLOWED".equals(e.getMessage())) {
                ErrorHandler.handle409(ctx, "OPERATION_NOT_ALLOWED", "Cannot check out reservation in current status");
            } else {
                ErrorHandler.handle400(ctx, "INVALID_REQUEST", e.getMessage());
            }
        } catch (Exception e) {
            ErrorHandler.handleException(ctx, e);
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(reservationToJson(reservation));
        } catch (IllegalArgumentException e) {
            if ("RESERVATION_NOT_FOUND".equals(e.getMessage())) {
                ErrorHandler.handle404(ctx, "RESERVATION_NOT_FOUND", "Reservation not found");
            } else if ("OPERATION_NOT_ALLOWED".equals(e.getMessage())) {
                ErrorHandler.handle409(ctx, "OPERATION_NOT_ALLOWED", "Cannot cancel reservation in current status");
            } else {
                ErrorHandler.handle400(ctx, "INVALID_REQUEST", e.getMessage());
            }
        } catch (Exception e) {
            ErrorHandler.handleException(ctx, e);
        }
    }

    public void getReceipt(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getStatus() == com.pousada.marealta.model.ReservationStatus.PENDING) {
                ErrorHandler.handle409(ctx, "OPERATION_NOT_ALLOWED", "Receipt is not available for pending reservations");
                return;
            }

            RoomDao roomDao = (RoomDao) ctx.attribute("roomDao");
            String roomType = roomDao.findByNumber(reservation.getRoomNumber()).getType().name();

            ReceiptDto receipt = new ReceiptDto(
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                roomType,
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
            );

            ctx.json(receipt);
        } catch (IllegalArgumentException e) {
            if ("RESERVATION_NOT_FOUND".equals(e.getMessage())) {
                ErrorHandler.handle404(ctx, "RESERVATION_NOT_FOUND", "Reservation not found");
            } else {
                ErrorHandler.handle400(ctx, "INVALID_REQUEST", e.getMessage());
            }
        } catch (Exception e) {
            ErrorHandler.handleException(ctx, e);
        }
    }

    private Object reservationToJson(Reservation reservation) {
        return new ReservationDto(reservation);
    }
}
