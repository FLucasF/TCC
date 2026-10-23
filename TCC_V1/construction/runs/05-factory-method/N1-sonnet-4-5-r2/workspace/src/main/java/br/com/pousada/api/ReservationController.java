package br.com.pousada.api;

import br.com.pousada.domain.*;
import br.com.pousada.exception.InvalidRequestException;
import br.com.pousada.service.ReservationService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        GuestIdentification identification = parseIdentification(request);
        RatePlan ratePlan = RatePlan.valueOf(request.ratePlan());

        Reservation reservation = reservationService.createReservation(
            request.roomNumber(),
            request.guestName(),
            request.guestEmail(),
            identification,
            request.checkIn(),
            request.checkOut(),
            request.guests(),
            ratePlan
        );

        ctx.status(201).json(toReservationResponse(reservation));
    }

    public void getReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(toReservationResponse(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(toReservationResponse(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(toReservationResponse(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toReservationResponse(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Document document = reservationService.checkOut(id);
        ctx.json(toDocumentResponse(document));
    }

    public void getDocument(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Document document = reservationService.getDocument(id);
        ctx.json(toDocumentResponse(document));
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.guestName() == null ||
            request.guestEmail() == null || request.checkIn() == null ||
            request.checkOut() == null || request.guests() == null ||
            request.ratePlan() == null) {
            throw new InvalidRequestException("Missing required fields");
        }

        int identificationCount = 0;
        if (request.guestCpf() != null) identificationCount++;
        if (request.companyCnpj() != null || request.companyName() != null) identificationCount++;
        if (request.guestPassport() != null || request.guestCountry() != null) identificationCount++;

        if (identificationCount != 1) {
            throw new InvalidRequestException("Exactly one identification type is required");
        }

        if (request.companyCnpj() != null && request.companyName() == null) {
            throw new InvalidRequestException("Company name is required with CNPJ");
        }
        if (request.companyName() != null && request.companyCnpj() == null) {
            throw new InvalidRequestException("CNPJ is required with company name");
        }

        if (request.guestPassport() != null && request.guestCountry() == null) {
            throw new InvalidRequestException("Country is required with passport");
        }
        if (request.guestCountry() != null && request.guestPassport() == null) {
            throw new InvalidRequestException("Passport is required with country");
        }
    }

    private GuestIdentification parseIdentification(CreateReservationRequest request) {
        if (request.guestCpf() != null) {
            return new CpfIdentification(request.guestCpf());
        } else if (request.companyCnpj() != null) {
            return new CnpjIdentification(request.companyCnpj(), request.companyName());
        } else {
            return new PassportIdentification(request.guestPassport(), request.guestCountry());
        }
    }

    private Map<String, Object> toReservationResponse(Reservation reservation) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", reservation.getId());
        response.put("roomNumber", reservation.getRoomNumber());
        response.put("guestName", reservation.getGuestName());
        response.put("guestEmail", reservation.getGuestEmail());

        switch (reservation.getIdentification()) {
            case CpfIdentification cpf -> {
                response.put("guestCpf", cpf.cpf());
                response.put("companyCnpj", null);
                response.put("companyName", null);
                response.put("guestPassport", null);
                response.put("guestCountry", null);
            }
            case CnpjIdentification cnpj -> {
                response.put("guestCpf", null);
                response.put("companyCnpj", cnpj.cnpj());
                response.put("companyName", cnpj.companyName());
                response.put("guestPassport", null);
                response.put("guestCountry", null);
            }
            case PassportIdentification passport -> {
                response.put("guestCpf", null);
                response.put("companyCnpj", null);
                response.put("companyName", null);
                response.put("guestPassport", passport.passport());
                response.put("guestCountry", passport.country());
            }
        }

        response.put("checkIn", reservation.getCheckIn());
        response.put("checkOut", reservation.getCheckOut());
        response.put("guests", reservation.getGuests());
        response.put("ratePlan", reservation.getRatePlan().name());
        response.put("nights", reservation.getNights());
        response.put("subtotal", reservation.getSubtotal());
        response.put("discount", reservation.getDiscount());
        response.put("total", reservation.getTotal());
        response.put("status", reservation.getStatus().name());
        response.put("amountPaid", reservation.getAmountPaid());
        response.put("refund", reservation.getRefund());

        return response;
    }

    private Map<String, Object> toDocumentResponse(Document document) {
        Map<String, Object> response = new HashMap<>();
        response.put("title", document.title());
        response.put("reservationId", document.reservationId());
        response.put("guestName", document.guestName());
        response.put("guestCpf", document.guestCpf());
        response.put("companyCnpj", document.companyCnpj());
        response.put("companyName", document.companyName());
        response.put("guestPassport", document.guestPassport());
        response.put("guestCountry", document.guestCountry());
        response.put("roomNumber", document.roomNumber());
        response.put("roomType", document.roomType());
        response.put("checkIn", document.checkIn());
        response.put("checkOut", document.checkOut());
        response.put("nights", document.nights());
        response.put("subtotal", document.subtotal());
        response.put("discount", document.discount());
        response.put("total", document.total());
        response.put("text", document.text());
        return response;
    }
}
