package br.com.mareaalta.api;

import br.com.mareaalta.domain.DocumentData;
import br.com.mareaalta.domain.RatePlan;
import br.com.mareaalta.domain.Reservation;
import br.com.mareaalta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;

public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequired(request.getRoomNumber(), "roomNumber");
        validateRequired(request.getGuestName(), "guestName");
        validateRequired(request.getGuestEmail(), "guestEmail");
        validateRequired(request.getCheckIn(), "checkIn");
        validateRequired(request.getCheckOut(), "checkOut");
        validateRequired(request.getGuests(), "guests");
        validateRequired(request.getRatePlan(), "ratePlan");

        LocalDate checkIn = ApiMapper.parseDate(request.getCheckIn());
        LocalDate checkOut = ApiMapper.parseDate(request.getCheckOut());

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        RatePlan ratePlan = ApiMapper.parseRatePlan(request.getRatePlan());

        Reservation reservation = reservationService.createReservation(
            request.getRoomNumber(),
            request.getGuestName(),
            request.getGuestEmail(),
            request.getGuestCpf(),
            request.getCompanyCnpj(),
            request.getCompanyName(),
            request.getGuestPassport(),
            request.getGuestCountry(),
            checkIn,
            checkOut,
            request.getGuests(),
            ratePlan
        );

        ctx.status(201).json(ApiMapper.toReservationResponse(reservation));
    }

    public void getReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id)
            .orElseThrow(() -> new br.com.mareaalta.service.ReservationNotFoundException("Reservation not found"));
        ctx.json(ApiMapper.toReservationResponse(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ApiMapper.toReservationResponse(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ApiMapper.toReservationResponse(reservation));
    }

    public void checkInReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkInReservation(id);
        ctx.json(ApiMapper.toReservationResponse(reservation));
    }

    public void checkOutReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOutReservation(id);
        DocumentData document = reservation.generateDocument();
        ctx.json(ApiMapper.toDocumentResponse(document));
    }

    public void getDocument(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        DocumentData document = reservationService.getDocument(id);
        ctx.json(ApiMapper.toDocumentResponse(document));
    }

    private void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is required");
        }
        if (value instanceof String && ((String) value).isBlank()) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is required");
        }
    }
}
