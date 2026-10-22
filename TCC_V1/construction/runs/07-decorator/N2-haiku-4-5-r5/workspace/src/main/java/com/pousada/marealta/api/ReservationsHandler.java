package com.pousada.marealta.api;

import com.google.gson.JsonObject;
import com.pousada.marealta.domain.RatePlan;
import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;
import com.pousada.marealta.service.ReservationService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationsHandler extends Handler {
    private final ReservationService reservationService;

    public ReservationsHandler(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            MessageRepository messageRepository
    ) {
        this.reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            messageRepository
        );
    }

    public void createReservation(Context ctx) {
        try {
            JsonObject json = gson.fromJson(ctx.body(), JsonObject.class);

            int roomNumber = getRequiredInt(json, "roomNumber");
            String guestName = getRequiredString(json, "guestName");
            String guestEmail = getRequiredString(json, "guestEmail");
            String guestCpf = getRequiredString(json, "guestCpf");
            String checkInStr = getRequiredString(json, "checkIn");
            String checkOutStr = getRequiredString(json, "checkOut");
            int guests = getRequiredInt(json, "guests");
            String ratePlanStr = getRequiredString(json, "ratePlan");

            LocalDate checkIn = parseDate(checkInStr);
            LocalDate checkOut = parseDate(checkOutStr);
            RatePlan ratePlan = RatePlan.fromString(ratePlanStr);

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(reservationToJson(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(reservationToJson(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            JsonObject json = gson.fromJson(ctx.body(), JsonObject.class);
            BigDecimal amountPaid = new BigDecimal(json.get("amount").getAsString());

            Reservation reservation = reservationService.confirmReservation(id, amountPaid);
            ctx.json(reservationToJson(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(reservationToJson(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);

            String roomType = getRoomType(reservation.getRoomNumber());
            String comprovante = reservationService.buildComprovante(reservation, roomType);

            JsonObject response = new JsonObject();
            response.addProperty("title", "Comprovante de reserva");
            response.addProperty("reservationId", reservation.getId());
            response.addProperty("guestName", reservation.getGuestName());
            response.addProperty("guestCpf", reservation.getGuestCpf());
            response.addProperty("roomNumber", reservation.getRoomNumber());
            response.addProperty("roomType", roomType);
            response.addProperty("checkIn", reservation.getCheckIn().toString());
            response.addProperty("checkOut", reservation.getCheckOut().toString());
            response.addProperty("nights", reservation.getNights());
            response.addProperty("subtotal", reservation.getSubtotal().doubleValue());
            response.addProperty("discount", reservation.getDiscount().doubleValue());
            response.addProperty("total", reservation.getTotal().doubleValue());

            ctx.json(response);
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getStatus().name().equals("PENDING")) {
                error(ctx, 409, "OPERATION_NOT_ALLOWED", "Document is not available for pending reservations");
                return;
            }

            String roomType = getRoomType(reservation.getRoomNumber());
            JsonObject response = new JsonObject();
            response.addProperty("title", "Comprovante de reserva");
            response.addProperty("reservationId", reservation.getId());
            response.addProperty("guestName", reservation.getGuestName());
            response.addProperty("guestCpf", reservation.getGuestCpf());
            response.addProperty("roomNumber", reservation.getRoomNumber());
            response.addProperty("roomType", roomType);
            response.addProperty("checkIn", reservation.getCheckIn().toString());
            response.addProperty("checkOut", reservation.getCheckOut().toString());
            response.addProperty("nights", reservation.getNights());
            response.addProperty("subtotal", reservation.getSubtotal().doubleValue());
            response.addProperty("discount", reservation.getDiscount().doubleValue());
            response.addProperty("total", reservation.getTotal().doubleValue());

            ctx.json(response);
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(reservationToJson(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    private JsonObject reservationToJson(Reservation reservation) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", reservation.getId());
        obj.addProperty("roomNumber", reservation.getRoomNumber());
        obj.addProperty("guestName", reservation.getGuestName());
        obj.addProperty("guestEmail", reservation.getGuestEmail());
        obj.addProperty("guestCpf", reservation.getGuestCpf());
        obj.addProperty("checkIn", reservation.getCheckIn().toString());
        obj.addProperty("checkOut", reservation.getCheckOut().toString());
        obj.addProperty("guests", reservation.getGuests());
        obj.addProperty("ratePlan", reservation.getRatePlan().getValue());
        obj.addProperty("nights", reservation.getNights());
        obj.addProperty("subtotal", reservation.getSubtotal().doubleValue());
        obj.addProperty("discount", reservation.getDiscount().doubleValue());
        obj.addProperty("total", reservation.getTotal().doubleValue());
        obj.addProperty("status", reservation.getStatus().name());
        obj.addProperty("amountPaid", reservation.getAmountPaid().doubleValue());

        if (reservation.getRefund() != null) {
            obj.addProperty("refund", reservation.getRefund().doubleValue());
        } else {
            obj.add("refund", null);
        }

        return obj;
    }

    private String getRoomType(int roomNumber) throws Exception {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }
}
