package br.com.maralta.controller;

import br.com.maralta.controller.dto.CreateReservationRequest;
import br.com.maralta.controller.dto.DocumentResponse;
import br.com.maralta.controller.dto.ReservationResponse;
import br.com.maralta.domain.RatePlanType;
import br.com.maralta.domain.Reservation;
import br.com.maralta.domain.Room;
import br.com.maralta.repository.RoomRepository;
import br.com.maralta.service.ReservationService;
import io.javalin.http.Context;

public class ReservationController {

    private final ReservationService reservationService;
    private final RoomRepository roomRepository;

    public ReservationController(ReservationService reservationService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        RatePlanType ratePlan = RatePlanType.valueOf(request.ratePlan());

        Reservation reservation = reservationService.createReservation(
            request.roomNumber(),
            request.guestName(),
            request.guestEmail(),
            request.guestCpf(),
            request.checkIn(),
            request.checkOut(),
            request.guests(),
            ratePlan,
            request.companyCode()
        );

        ctx.status(201).json(ReservationResponse.from(reservation));
    }

    public void getReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkOut(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(DocumentResponse.from(reservation, room));
    }

    public void getDocument(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);

        if (reservation.getAmountPaid().compareTo(java.math.BigDecimal.ZERO) == 0) {
            throw new br.com.maralta.service.OperationNotAllowedException(
                "Document is only available for paid reservations");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        ctx.json(DocumentResponse.from(reservation, room));
    }
}
