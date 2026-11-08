package br.com.marealta.api.controller;

import br.com.marealta.api.dto.CreateReservationRequest;
import br.com.marealta.api.dto.DocumentResponse;
import br.com.marealta.api.dto.ReservationResponse;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.RoomType;
import br.com.marealta.repository.RoomRepository;
import br.com.marealta.service.DocumentService;
import br.com.marealta.service.ReservationService;
import io.javalin.http.Context;

public class ReservationController {
    private final ReservationService reservationService;
    private final DocumentService documentService;
    private final RoomRepository roomRepository;

    public ReservationController(
            ReservationService reservationService,
            DocumentService documentService,
            RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.documentService = documentService;
        this.roomRepository = roomRepository;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(request.roomNumber());
        reservation.setGuestName(request.guestName());
        reservation.setGuestEmail(request.guestEmail());
        reservation.setGuestCpf(request.guestCpf());
        reservation.setCheckIn(request.checkIn());
        reservation.setCheckOut(request.checkOut());
        reservation.setGuests(request.guests());
        reservation.setRatePlan(request.ratePlan());

        Reservation created = reservationService.createReservation(reservation);

        ctx.status(201).json(ReservationResponse.from(created));
    }

    public void getReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.findReservationById(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        DocumentResponse document = buildDocument(reservation);
        ctx.json(document);
    }

    public void getDocument(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.findReservationById(id);

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            throw new IllegalStateException("Document not available for pending reservation");
        }

        DocumentResponse document = buildDocument(reservation);
        ctx.json(document);
    }

    private DocumentResponse buildDocument(Reservation reservation) {
        RoomType roomType = roomRepository.findByNumber(reservation.getRoomNumber())
            .map(room -> room.getType())
            .orElse(null);

        return new DocumentResponse(
            "Comprovante de reserva",
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
    }
}
