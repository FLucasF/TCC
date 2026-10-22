package com.pousadamaralta.api.controller;

import com.pousadamaralta.api.dto.CreateReservationRequest;
import com.pousadamaralta.api.dto.DocumentResponse;
import com.pousadamaralta.api.dto.ErrorResponse;
import com.pousadamaralta.api.dto.ReservationResponse;
import com.pousadamaralta.domain.identification.CnpjIdentification;
import com.pousadamaralta.domain.identification.CpfIdentification;
import com.pousadamaralta.domain.identification.GuestIdentification;
import com.pousadamaralta.domain.identification.PassportIdentification;
import com.pousadamaralta.domain.model.RatePlanType;
import com.pousadamaralta.domain.model.Reservation;
import com.pousadamaralta.domain.model.Room;
import com.pousadamaralta.domain.service.ReservationService;
import com.pousadamaralta.domain.service.RoomService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReservationController(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public void create(Context ctx) {
        CreateReservationRequest request;

        try {
            request = ctx.bodyAsClass(CreateReservationRequest.class);
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid request body"));
            return;
        }

        if (!isValidRequest(request)) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid request parameters"));
            return;
        }

        GuestIdentification identification;
        try {
            identification = parseIdentification(request);
        } catch (InvalidIdentificationException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            return;
        }

        RatePlanType ratePlanType;
        try {
            ratePlanType = RatePlanType.valueOf(request.ratePlan());
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
            return;
        }

        try {
            Reservation reservation = reservationService.createReservation(
                request.roomNumber(),
                request.guestName(),
                request.guestEmail(),
                identification,
                request.checkIn(),
                request.checkOut(),
                request.guests(),
                ratePlanType
            );

            ctx.status(HttpStatus.CREATED)
                .json(ReservationResponse.from(reservation));
        } catch (RoomService.RoomNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void get(Context ctx) {
        Long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            return;
        }

        try {
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void confirm(Context ctx) {
        Long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            return;
        }

        try {
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void cancel(Context ctx) {
        Long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            return;
        }

        try {
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        Long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            return;
        }

        try {
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        Long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            return;
        }

        try {
            Reservation reservation = reservationService.checkOut(id);
            Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

            String documentText = reservation.getIdentification().generateDocument(
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getRoomNumber(),
                room.getType(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
            );

            ctx.json(DocumentResponse.from(reservation, room, documentText));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT)
                .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        Long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            return;
        }

        try {
            Reservation reservation = reservationService.getReservation(id);

            if (!reservation.isPaid()) {
                ctx.status(HttpStatus.CONFLICT)
                    .json(new ErrorResponse("OPERATION_NOT_ALLOWED",
                        "Document only available for paid reservations"));
                return;
            }

            Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

            String documentText = reservation.getIdentification().generateDocument(
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getRoomNumber(),
                room.getType(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
            );

            ctx.json(DocumentResponse.from(reservation, room, documentText));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
                .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    private boolean isValidRequest(CreateReservationRequest request) {
        return request.roomNumber() != null &&
               request.guestName() != null &&
               request.guestEmail() != null &&
               request.checkIn() != null &&
               request.checkOut() != null &&
               request.guests() != null &&
               request.ratePlan() != null;
    }

    private GuestIdentification parseIdentification(CreateReservationRequest request) {
        int identificationCount = 0;

        if (request.guestCpf() != null) identificationCount++;
        if (request.companyCnpj() != null) identificationCount++;
        if (request.guestPassport() != null) identificationCount++;

        if (identificationCount != 1) {
            throw new InvalidIdentificationException(
                "Exactly one identification type must be provided"
            );
        }

        if (request.guestCpf() != null) {
            return new CpfIdentification(request.guestCpf());
        }

        if (request.companyCnpj() != null) {
            if (request.companyName() == null) {
                throw new InvalidIdentificationException(
                    "Company name is required when CNPJ is provided"
                );
            }
            return new CnpjIdentification(request.companyCnpj(), request.companyName());
        }

        if (request.guestPassport() != null) {
            if (request.guestCountry() == null) {
                throw new InvalidIdentificationException(
                    "Country is required when passport is provided"
                );
            }
            return new PassportIdentification(request.guestPassport(), request.guestCountry());
        }

        throw new InvalidIdentificationException("No valid identification provided");
    }

    private static class InvalidIdentificationException extends RuntimeException {
        public InvalidIdentificationException(String message) {
            super(message);
        }
    }
}
