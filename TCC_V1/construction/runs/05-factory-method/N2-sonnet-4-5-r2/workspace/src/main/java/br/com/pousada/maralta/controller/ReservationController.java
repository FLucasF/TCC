package br.com.pousada.maralta.controller;

import br.com.pousada.maralta.domain.*;
import br.com.pousada.maralta.guesttype.GuestType;
import br.com.pousada.maralta.guesttype.GuestTypeFactory;
import br.com.pousada.maralta.service.ReservationService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            String roomNumber = (String) body.get("roomNumber");
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String checkInStr = (String) body.get("checkIn");
            String checkOutStr = (String) body.get("checkOut");
            Integer guests = getInteger(body, "guests");
            String ratePlanStr = (String) body.get("ratePlan");

            if (roomNumber == null || guestName == null || guestEmail == null ||
                checkInStr == null || checkOutStr == null || guests == null || ratePlanStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            GuestType guestType = GuestTypeFactory.createFromRequest(body);
            if (guestType == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid guest identification"));
                return;
            }

            LocalDate checkIn;
            LocalDate checkOut;
            try {
                checkIn = LocalDate.parse(checkInStr);
                checkOut = LocalDate.parse(checkOutStr);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            if (!checkOut.isAfter(checkIn)) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Check-out must be after check-in"));
                return;
            }

            RatePlan ratePlan;
            try {
                ratePlan = RatePlan.valueOf(ratePlanStr);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
                return;
            }

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestType,
                checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(toJson(reservation));

        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room not available"));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Guests exceed room capacity"));
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(toJson(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(toJson(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed"));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(toJson(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed"));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(toJson(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed"));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Document document = reservationService.checkOut(id);
            ctx.json(toDocumentJson(document));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed"));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Document document = reservationService.getDocument(id);
            ctx.json(toDocumentJson(document));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed"));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private Map<String, Object> toJson(Reservation reservation) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", reservation.getId());
        json.put("roomNumber", reservation.getRoomNumber());
        json.put("guestName", reservation.getGuestName());
        json.put("guestEmail", reservation.getGuestEmail());
        json.put("guestCpf", reservation.getGuestType().getGuestCpf());
        json.put("companyCnpj", reservation.getGuestType().getCompanyCnpj());
        json.put("companyName", reservation.getGuestType().getCompanyName());
        json.put("guestPassport", reservation.getGuestType().getGuestPassport());
        json.put("guestCountry", reservation.getGuestType().getGuestCountry());
        json.put("checkIn", reservation.getCheckIn().toString());
        json.put("checkOut", reservation.getCheckOut().toString());
        json.put("guests", reservation.getGuests());
        json.put("ratePlan", reservation.getRatePlan().name());
        json.put("nights", reservation.getNights());
        json.put("subtotal", toDouble(reservation.getSubtotal()));
        json.put("discount", toDouble(reservation.getDiscount()));
        json.put("total", toDouble(reservation.getTotal()));
        json.put("status", reservation.getStatus().name());
        json.put("amountPaid", toDouble(reservation.getAmountPaid()));
        json.put("refund", reservation.getRefund() != null ? toDouble(reservation.getRefund()) : null);
        return json;
    }

    private Map<String, Object> toDocumentJson(Document document) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("title", document.getTitle());
        json.put("reservationId", document.getReservationId());
        json.put("guestName", document.getGuestName());
        json.put("guestCpf", document.getGuestCpf());
        json.put("companyCnpj", document.getCompanyCnpj());
        json.put("companyName", document.getCompanyName());
        json.put("guestPassport", document.getGuestPassport());
        json.put("guestCountry", document.getGuestCountry());
        json.put("roomNumber", document.getRoomNumber());
        json.put("roomType", document.getRoomType());
        json.put("checkIn", document.getCheckIn().toString());
        json.put("checkOut", document.getCheckOut().toString());
        json.put("nights", document.getNights());
        json.put("subtotal", toDouble(document.getSubtotal()));
        json.put("discount", toDouble(document.getDiscount()));
        json.put("total", toDouble(document.getTotal()));
        json.put("text", document.getText());
        return json;
    }

    private double toDouble(BigDecimal value) {
        return value.doubleValue();
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
}
