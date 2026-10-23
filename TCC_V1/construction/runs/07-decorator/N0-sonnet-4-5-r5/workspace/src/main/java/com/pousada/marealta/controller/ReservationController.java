package com.pousada.marealta.controller;

import com.pousada.marealta.domain.RatePlan;
import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.dto.CreateReservationRequest;
import com.pousada.marealta.dto.DocumentResponse;
import com.pousada.marealta.dto.ErrorResponse;
import com.pousada.marealta.dto.ReservationResponse;
import com.pousada.marealta.service.ReservationService;
import com.pousada.marealta.service.RoomService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReservationController(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            // Validações básicas
            if (request.roomNumber() == null || request.guestName() == null ||
                request.guestEmail() == null || request.guestCpf() == null ||
                request.checkIn() == null || request.checkOut() == null ||
                request.ratePlan() == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            if (request.guests() < 1) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Number of guests must be at least 1"));
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

            RatePlan ratePlan;
            try {
                ratePlan = RatePlan.valueOf(request.ratePlan());
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
                return;
            }

            Reservation reservation = new Reservation();
            reservation.setRoomNumber(request.roomNumber());
            reservation.setGuestName(request.guestName());
            reservation.setGuestEmail(request.guestEmail());
            reservation.setGuestCpf(request.guestCpf());
            reservation.setCheckIn(checkIn);
            reservation.setCheckOut(checkOut);
            reservation.setGuests(request.guests());
            reservation.setRatePlan(ratePlan);

            Reservation created = reservationService.createReservation(reservation);
            ctx.status(201).json(ReservationResponse.from(created));

        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for these dates"));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot confirm this reservation"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot cancel this reservation"));
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
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check in this reservation"));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            String document = reservationService.checkOut(id);
            Reservation reservation = reservationService.getReservation(id);
            Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
            ctx.json(DocumentResponse.from(reservation, room));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check out this reservation"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            String document = reservationService.getDocument(id);
            Reservation reservation = reservationService.getReservation(id);
            Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
            ctx.json(DocumentResponse.from(reservation, room));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
        }
    }
}
