package com.pousadamarealta.web.controller;

import com.pousadamarealta.domain.model.RatePlan;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.web.dto.CreateReservationRequest;
import com.pousadamarealta.web.dto.DocumentDTO;
import com.pousadamarealta.web.dto.ErrorResponse;
import com.pousadamarealta.web.dto.ReservationDTO;
import com.pousadamarealta.web.mapper.DomainMapper;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

            if (req.getRoomNumber() == null || req.getGuestName() == null ||
                req.getGuestEmail() == null || req.getGuestCpf() == null ||
                req.getCheckIn() == null || req.getCheckOut() == null ||
                req.getGuests() == null || req.getRatePlan() == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            LocalDate checkIn;
            LocalDate checkOut;
            try {
                checkIn = LocalDate.parse(req.getCheckIn());
                checkOut = LocalDate.parse(req.getCheckOut());
            } catch (DateTimeParseException e) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            RatePlan ratePlan;
            try {
                ratePlan = RatePlan.valueOf(req.getRatePlan());
            } catch (IllegalArgumentException e) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
                return;
            }

            Reservation reservation = reservationService.createReservation(
                req.getRoomNumber(),
                req.getGuestName(),
                req.getGuestEmail(),
                req.getGuestCpf(),
                checkIn,
                checkOut,
                req.getGuests(),
                ratePlan
            );

            ctx.status(HttpStatus.CREATED).json(DomainMapper.toDTO(reservation));

        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
               .json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(DomainMapper.toDTO(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(DomainMapper.toDTO(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(DomainMapper.toDTO(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(DomainMapper.toDTO(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            ctx.json(DomainMapper.toDocumentDTO(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (!reservation.isPaid()) {
                ctx.status(HttpStatus.CONFLICT)
                   .json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
                return;
            }

            ctx.json(DomainMapper.toDocumentDTO(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }
}
