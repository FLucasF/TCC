package com.pousadamareal.controller;

import com.pousadamareal.api.*;
import com.pousadamareal.model.*;
import com.pousadamareal.service.ReservationService;
import io.javalin.http.Context;

public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

        if (request.getRoomNumber() == null || request.getGuestName() == null ||
            request.getGuestEmail() == null || request.getCheckIn() == null ||
            request.getCheckOut() == null || request.getGuests() == null ||
            request.getRatePlan() == null) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
            return;
        }

        GuestIdentification identification;
        int identificationCount = 0;

        if (request.getGuestCpf() != null) {
            identification = new CpfIdentification(request.getGuestCpf());
            identificationCount++;
        } else if (request.getCompanyCnpj() != null && request.getCompanyName() != null) {
            identification = new CnpjIdentification(request.getCompanyCnpj(), request.getCompanyName());
            identificationCount++;
        } else if (request.getGuestPassport() != null && request.getGuestCountry() != null) {
            identification = new PassportIdentification(request.getGuestPassport(), request.getGuestCountry());
            identificationCount++;
        } else {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid guest identification"));
            return;
        }

        if ((request.getGuestCpf() != null ? 1 : 0) +
            (request.getCompanyCnpj() != null ? 1 : 0) +
            (request.getGuestPassport() != null ? 1 : 0) > 1) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Multiple identification types provided"));
            return;
        }

        RatePlan ratePlan;
        try {
            ratePlan = RatePlan.valueOf(request.getRatePlan());
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
            return;
        }

        try {
            Reservation reservation = reservationService.createReservation(
                request.getRoomNumber(),
                request.getGuestName(),
                request.getGuestEmail(),
                identification,
                request.getCheckIn(),
                request.getCheckOut(),
                request.getGuests(),
                ratePlan
            );
            ctx.status(201).json(new ReservationResponse(reservation));
        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(new ReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(new ReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(new ReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(new ReservationResponse(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Document document = reservationService.checkOut(id);
            ctx.json(new DocumentResponse(document));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Document document = reservationService.getDocument(id);
            ctx.json(new DocumentResponse(document));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }
}
