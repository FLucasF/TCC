package com.pousadamarealta.api;

import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final MessageService messageService;

    public ReservationController(ReservationService reservationService, MessageService messageService) {
        this.reservationService = reservationService;
        this.messageService = messageService;
    }

    public void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            String roomNumber = (String) body.get("roomNumber");
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String guestCpf = (String) body.get("guestCpf");
            String checkInStr = (String) body.get("checkIn");
            String checkOutStr = (String) body.get("checkOut");
            Integer guests = (Integer) body.get("guests");
            String ratePlanStr = (String) body.get("ratePlan");

            if (roomNumber == null || guestName == null || guestEmail == null || guestCpf == null ||
                checkInStr == null || checkOutStr == null || guests == null || ratePlanStr == null) {
                error(ctx, 400, "INVALID_REQUEST", "Missing required fields");
                return;
            }

            LocalDate checkIn;
            LocalDate checkOut;
            try {
                checkIn = LocalDate.parse(checkInStr);
                checkOut = LocalDate.parse(checkOutStr);
            } catch (DateTimeParseException e) {
                error(ctx, 400, "INVALID_REQUEST", "Invalid date format");
                return;
            }

            RatePlan ratePlan;
            try {
                ratePlan = RatePlan.valueOf(ratePlanStr);
            } catch (IllegalArgumentException e) {
                error(ctx, 400, "INVALID_REQUEST", "Invalid rate plan");
                return;
            }

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan);

            ctx.status(201).json(toDto(reservation));

        } catch (ReservationService.InvalidRequestException e) {
            error(ctx, 400, "INVALID_REQUEST", e.getMessage());
        } catch (ReservationService.RoomNotFoundException e) {
            error(ctx, 404, "ROOM_NOT_FOUND", e.getMessage());
        } catch (ReservationService.RoomUnavailableException e) {
            error(ctx, 409, "ROOM_UNAVAILABLE", e.getMessage());
        } catch (ReservationService.CapacityExceededException e) {
            error(ctx, 422, "CAPACITY_EXCEEDED", e.getMessage());
        } catch (Exception e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid request body");
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getById(id);
            ctx.json(toDto(reservation));
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        } catch (ReservationService.ReservationNotFoundException e) {
            error(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage());
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            reservationService.confirm(id);
            Reservation reservation = reservationService.getById(id);

            Document document = Document.fromReservation(reservation);
            messageService.sendConfirmationEmail(reservation.guestEmail(), reservation.guestName(), document);

            ctx.json(toDto(reservation));
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        } catch (ReservationService.ReservationNotFoundException e) {
            error(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (ReservationService.OperationNotAllowedException e) {
            error(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            reservationService.cancel(id);
            Reservation reservation = reservationService.getById(id);
            ctx.json(toDto(reservation));
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        } catch (ReservationService.ReservationNotFoundException e) {
            error(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (ReservationService.OperationNotAllowedException e) {
            error(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            reservationService.checkIn(id);
            Reservation reservation = reservationService.getById(id);
            ctx.json(toDto(reservation));
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        } catch (ReservationService.ReservationNotFoundException e) {
            error(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (ReservationService.OperationNotAllowedException e) {
            error(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            reservationService.checkOut(id);
            Reservation reservation = reservationService.getById(id);
            Document document = Document.fromReservation(reservation);
            ctx.json(toDocumentDto(document));
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        } catch (ReservationService.ReservationNotFoundException e) {
            error(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (ReservationService.OperationNotAllowedException e) {
            error(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getById(id);

            if (reservation.status() == ReservationStatus.PENDING) {
                error(ctx, 409, "OPERATION_NOT_ALLOWED", "Document only available for paid reservations");
                return;
            }

            Document document = Document.fromReservation(reservation);
            ctx.json(toDocumentDto(document));
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        } catch (ReservationService.ReservationNotFoundException e) {
            error(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage());
        }
    }

    private Map<String, Object> toDto(Reservation reservation) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", reservation.id());
        dto.put("roomNumber", reservation.roomNumber());
        dto.put("guestName", reservation.guestName());
        dto.put("guestEmail", reservation.guestEmail());
        dto.put("guestCpf", reservation.guestCpf());
        dto.put("checkIn", reservation.checkIn().toString());
        dto.put("checkOut", reservation.checkOut().toString());
        dto.put("guests", reservation.guests());
        dto.put("ratePlan", reservation.ratePlan().name());
        dto.put("nights", reservation.nights());
        dto.put("subtotal", reservation.subtotal().doubleValue());
        dto.put("discount", reservation.discount().doubleValue());
        dto.put("total", reservation.total().doubleValue());
        dto.put("status", reservation.status().name());
        dto.put("amountPaid", reservation.amountPaid().doubleValue());
        dto.put("refund", reservation.refund() != null ? reservation.refund().doubleValue() : null);
        return dto;
    }

    private Map<String, Object> toDocumentDto(Document document) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("title", document.title());
        dto.put("reservationId", document.reservationId());
        dto.put("guestName", document.guestName());
        dto.put("guestCpf", document.guestCpf());
        dto.put("roomNumber", document.roomNumber());
        dto.put("roomType", document.roomType().name());
        dto.put("checkIn", document.checkIn().toString());
        dto.put("checkOut", document.checkOut().toString());
        dto.put("nights", document.nights());
        dto.put("subtotal", document.subtotal().doubleValue());
        dto.put("discount", document.discount().doubleValue());
        dto.put("total", document.total().doubleValue());
        return dto;
    }

    private void error(Context ctx, int status, String code, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", code);
        error.put("message", message);
        ctx.status(status).json(error);
    }
}
