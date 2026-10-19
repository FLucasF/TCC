package com.pousada.api.controller;

import com.pousada.api.dto.CreateReservationRequest;
import com.pousada.api.dto.DocumentResponse;
import com.pousada.api.dto.ReservationResponse;
import com.pousada.domain.model.Reservation;
import com.pousada.domain.model.Room;
import com.pousada.domain.service.DocumentService;
import com.pousada.domain.service.ReservationService;
import com.pousada.domain.service.RoomService;
import com.pousada.domain.validation.ValidationException;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class ReservationController {
    private final ReservationService reservationService;
    private final DocumentService documentService;
    private final RoomService roomService;

    public ReservationController(ReservationService reservationService,
                                DocumentService documentService,
                                RoomService roomService) {
        this.reservationService = reservationService;
        this.documentService = documentService;
        this.roomService = roomService;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        Reservation reservation = reservationService.createReservation(
            request.roomNumber(),
            request.guestName(),
            request.guestEmail(),
            request.guestCpf(),
            request.checkIn(),
            request.checkOut(),
            request.guests(),
            request.ratePlan()
        );

        ctx.status(HttpStatus.CREATED);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void getReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        documentService.sendConfirmationEmail(reservation);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void checkInReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkInReservation(id);
        ctx.json(ReservationResponse.from(reservation));
    }

    public void checkOutReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkOutReservation(id);
        String document = documentService.generateDocument(reservation);
        ctx.json(buildDocumentResponse(reservation, document));
    }

    public void getDocument(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);
        String document = documentService.generateDocument(reservation);
        ctx.json(buildDocumentResponse(reservation, document));
    }

    private DocumentResponse buildDocumentResponse(Reservation reservation, String document) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

        return new DocumentResponse(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuest().getName(),
            reservation.getGuest().getCpf(),
            reservation.getRoomNumber(),
            room.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.guestName() == null ||
            request.guestEmail() == null || request.guestCpf() == null ||
            request.checkIn() == null || request.checkOut() == null ||
            request.guests() == null || request.ratePlan() == null) {
            throw new ValidationException("INVALID_REQUEST", "Missing required fields");
        }
    }
}
