package com.pousadamarealta.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.pousadamarealta.model.*;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.ServiceException;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationHandler {
    private final ReservationService reservationService;

    public ReservationHandler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        try {
            JsonNode json = ctx.bodyAsClass(JsonNode.class);

            if (!json.has("roomNumber") || !json.has("guestName") || !json.has("guestEmail") ||
                !json.has("checkIn") || !json.has("checkOut") || !json.has("guests") ||
                !json.has("ratePlan")) {
                throw new ServiceException("INVALID_REQUEST", "Missing required fields", 400);
            }

            String roomNumber = json.get("roomNumber").asText();
            String guestName = json.get("guestName").asText();
            String guestEmail = json.get("guestEmail").asText();
            LocalDate checkIn = LocalDate.parse(json.get("checkIn").asText());
            LocalDate checkOut = LocalDate.parse(json.get("checkOut").asText());
            int guests = json.get("guests").asInt();
            RatePlan ratePlan = RatePlan.valueOf(json.get("ratePlan").asText());

            GuestIdentification identification = parseIdentification(json);

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, identification,
                checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(toDto(reservation));
        } catch (DateTimeParseException e) {
            throw new ServiceException("INVALID_REQUEST", "Invalid date format", 400);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("INVALID_REQUEST", "Invalid field value", 400);
        }
    }

    private GuestIdentification parseIdentification(JsonNode json) {
        boolean hasCpf = json.has("guestCpf") && !json.get("guestCpf").isNull();
        boolean hasCnpj = json.has("companyCnpj") && !json.get("companyCnpj").isNull();
        boolean hasCompanyName = json.has("companyName") && !json.get("companyName").isNull();
        boolean hasPassport = json.has("guestPassport") && !json.get("guestPassport").isNull();
        boolean hasCountry = json.has("guestCountry") && !json.get("guestCountry").isNull();

        int identificationCount = 0;
        if (hasCpf) identificationCount++;
        if (hasCnpj && hasCompanyName) identificationCount++;
        if (hasPassport && hasCountry) identificationCount++;

        if (identificationCount != 1) {
            throw new ServiceException("INVALID_REQUEST",
                "Must provide exactly one type of identification", 400);
        }

        if (hasCpf) {
            return GuestIdentification.withCpf(json.get("guestCpf").asText());
        } else if (hasCnpj && hasCompanyName) {
            return GuestIdentification.withCompany(
                json.get("companyCnpj").asText(),
                json.get("companyName").asText()
            );
        } else {
            return GuestIdentification.withPassport(
                json.get("guestPassport").asText(),
                json.get("guestCountry").asText()
            );
        }
    }

    public void getReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id)
            .orElseThrow(() -> new ServiceException("RESERVATION_NOT_FOUND", "Reservation not found", 404));
        ctx.json(toDto(reservation));
    }

    public void confirmReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(toDto(reservation));
    }

    public void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(toDto(reservation));
    }

    public void checkIn(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toDto(reservation));
    }

    public void checkOut(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Document document = reservationService.checkOut(id);
        ctx.json(toDocumentDto(document));
    }

    public void getDocument(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Document document = reservationService.getDocument(id);
        ctx.json(toDocumentDto(document));
    }

    private Map<String, Object> toDto(Reservation reservation) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", reservation.getId());
        dto.put("roomNumber", reservation.getRoomNumber());
        dto.put("guestName", reservation.getGuestName());
        dto.put("guestEmail", reservation.getGuestEmail());

        GuestIdentification id = reservation.getIdentification();
        dto.put("guestCpf", id.getGuestCpf());
        dto.put("companyCnpj", id.getCompanyCnpj());
        dto.put("companyName", id.getCompanyName());
        dto.put("guestPassport", id.getGuestPassport());
        dto.put("guestCountry", id.getGuestCountry());

        dto.put("checkIn", reservation.getCheckIn().toString());
        dto.put("checkOut", reservation.getCheckOut().toString());
        dto.put("guests", reservation.getGuests());
        dto.put("ratePlan", reservation.getRatePlan().toString());
        dto.put("nights", reservation.getNights());
        dto.put("subtotal", reservation.getSubtotal());
        dto.put("discount", reservation.getDiscount());
        dto.put("total", reservation.getTotal());
        dto.put("status", reservation.getStatus().toString());
        dto.put("amountPaid", reservation.getAmountPaid());
        dto.put("refund", reservation.getRefund());

        return dto;
    }

    private Map<String, Object> toDocumentDto(Document document) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("title", document.getTitle());
        dto.put("reservationId", document.getReservationId());
        dto.put("guestName", document.getGuestName());
        dto.put("guestCpf", document.getGuestCpf());
        dto.put("companyCnpj", document.getCompanyCnpj());
        dto.put("companyName", document.getCompanyName());
        dto.put("guestPassport", document.getGuestPassport());
        dto.put("guestCountry", document.getGuestCountry());
        dto.put("roomNumber", document.getRoomNumber());
        dto.put("roomType", document.getRoomType().toString());
        dto.put("checkIn", document.getCheckIn().toString());
        dto.put("checkOut", document.getCheckOut().toString());
        dto.put("nights", document.getNights());
        dto.put("subtotal", document.getSubtotal());
        dto.put("discount", document.getDiscount());
        dto.put("total", document.getTotal());
        dto.put("text", document.getText());
        return dto;
    }
}
