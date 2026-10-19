package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.dto.CreateReservationRequest;
import com.pousadamarealta.api.dto.DocumentResponse;
import com.pousadamarealta.api.dto.ReservationResponse;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.InvalidRequestException;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

public class ReservationController {
    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReservationController(ReservationService reservationService,
                                ReservationRepository reservationRepository,
                                RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        validateRequest(request);

        GuestIdentification identification = buildIdentification(request);

        Reservation reservation = reservationService.createReservation(
            request.roomNumber(),
            request.guestName(),
            request.guestEmail(),
            identification,
            request.checkIn(),
            request.checkOut(),
            request.guests(),
            request.ratePlan()
        );

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        ctx.status(201).json(ReservationResponse.from(reservation, room));
    }

    public void getReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new com.pousadamarealta.service.ReservationNotFoundException());

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        ctx.json(ReservationResponse.from(reservation, room));
    }

    public void confirmReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        ctx.json(ReservationResponse.from(reservation, room));
    }

    public void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        ctx.json(ReservationResponse.from(reservation, room));
    }

    public void checkIn(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        ctx.json(ReservationResponse.from(reservation, room));
    }

    public void checkOut(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Document document = reservationService.checkOut(id);
        ctx.json(DocumentResponse.from(document));
    }

    public void getDocument(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Document document = reservationService.getDocument(id);
        ctx.json(DocumentResponse.from(document));
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.checkOut().isBefore(request.checkIn()) ||
            request.checkOut().equals(request.checkIn())) {
            throw new InvalidRequestException("Invalid dates");
        }

        int identificationCount = 0;
        if (request.guestCpf() != null) identificationCount++;
        if (request.companyCnpj() != null || request.companyName() != null) identificationCount++;
        if (request.guestPassport() != null || request.guestCountry() != null) identificationCount++;

        if (identificationCount != 1) {
            throw new InvalidRequestException("Invalid identification");
        }

        if (request.companyCnpj() != null && request.companyName() == null) {
            throw new InvalidRequestException("Company name required with CNPJ");
        }
        if (request.companyName() != null && request.companyCnpj() == null) {
            throw new InvalidRequestException("CNPJ required with company name");
        }
        if (request.guestPassport() != null && request.guestCountry() == null) {
            throw new InvalidRequestException("Country required with passport");
        }
        if (request.guestCountry() != null && request.guestPassport() == null) {
            throw new InvalidRequestException("Passport required with country");
        }
    }

    private GuestIdentification buildIdentification(CreateReservationRequest request) {
        if (request.guestCpf() != null) {
            return new CpfIdentification(request.guestCpf());
        }
        if (request.companyCnpj() != null) {
            return new CnpjIdentification(request.companyCnpj(), request.companyName());
        }
        if (request.guestPassport() != null) {
            return new PassportIdentification(request.guestPassport(), request.guestCountry());
        }
        throw new InvalidRequestException("No valid identification provided");
    }
}
