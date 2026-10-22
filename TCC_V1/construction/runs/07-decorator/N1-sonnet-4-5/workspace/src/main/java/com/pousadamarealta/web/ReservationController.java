package com.pousadamarealta.web;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.web.dto.CreateReservationRequest;
import com.pousadamarealta.web.dto.DocumentResponse;
import com.pousadamarealta.web.dto.ErrorResponse;
import com.pousadamarealta.web.dto.ReservationResponse;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;

    public ReservationController(ReservationService reservationService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            if (request.roomNumber() == null || request.guestName() == null ||
                    request.guestEmail() == null || request.guestCpf() == null ||
                    request.checkIn() == null || request.checkOut() == null ||
                    request.guests() == null || request.ratePlan() == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            LocalDate checkIn;
            LocalDate checkOut;
            try {
                checkIn = LocalDate.parse(request.checkIn());
                checkOut = LocalDate.parse(request.checkOut());
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            if (!checkOut.isAfter(checkIn)) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Check-out must be after check-in"));
                return;
            }

            if (request.guests() < 1) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "At least one guest is required"));
                return;
            }

            RatePlan ratePlan;
            try {
                ratePlan = RatePlan.valueOf(request.ratePlan());
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
                return;
            }

            Reservation reservation = reservationService.create(
                    request.roomNumber(),
                    request.guestName(),
                    request.guestEmail(),
                    request.guestCpf(),
                    checkIn,
                    checkOut,
                    request.guests(),
                    ratePlan
            );

            ctx.status(201).json(ReservationResponse.from(reservation));

        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request body"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.findById(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirm(id);
            ctx.json(ReservationResponse.from(reservation));
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
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancel(id);
            ctx.json(ReservationResponse.from(reservation));
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
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.from(reservation));
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
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            ctx.json(DocumentResponse.from(reservation, room));
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
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.findById(id);

            if (!reservation.isPaid()) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED",
                        "Document is only available for paid reservations"));
                return;
            }

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            ctx.json(DocumentResponse.from(reservation, room));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }
}
