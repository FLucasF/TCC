package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.dto.CreateReservationRequest;
import com.pousadamarealta.api.dto.DocumentDTO;
import com.pousadamarealta.api.dto.ReservationDTO;
import com.pousadamarealta.api.error.ErrorCode;
import com.pousadamarealta.api.error.ErrorResponse;
import com.pousadamarealta.domain.model.*;
import com.pousadamarealta.domain.service.ReservationService;
import com.pousadamarealta.domain.service.RoomService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReservationController(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            if (request.roomNumber() == null || request.guestName() == null ||
                request.guestEmail() == null || request.guestCpf() == null ||
                request.checkIn() == null || request.checkOut() == null || request.ratePlan() == null) {
                ctx.status(400).json(new ErrorResponse(
                    ErrorCode.INVALID_REQUEST.name(),
                    "Missing required fields"
                ));
                return;
            }

            LocalDate checkIn;
            LocalDate checkOut;
            try {
                checkIn = LocalDate.parse(request.checkIn(), DATE_FORMATTER);
                checkOut = LocalDate.parse(request.checkOut(), DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse(
                    ErrorCode.INVALID_REQUEST.name(),
                    "Invalid date format"
                ));
                return;
            }

            if (!checkOut.isAfter(checkIn)) {
                ctx.status(400).json(new ErrorResponse(
                    ErrorCode.INVALID_REQUEST.name(),
                    "Check-out must be after check-in"
                ));
                return;
            }

            if (request.guests() < 1) {
                ctx.status(400).json(new ErrorResponse(
                    ErrorCode.INVALID_REQUEST.name(),
                    "At least one guest is required"
                ));
                return;
            }

            Room room = roomService.getRoomByNumber(request.roomNumber()).orElse(null);
            if (room == null) {
                ctx.status(404).json(new ErrorResponse(
                    ErrorCode.ROOM_NOT_FOUND.name(),
                    "Room not found"
                ));
                return;
            }

            if (request.guests() > room.getCapacity()) {
                ctx.status(422).json(new ErrorResponse(
                    ErrorCode.CAPACITY_EXCEEDED.name(),
                    "Number of guests exceeds room capacity"
                ));
                return;
            }

            if (reservationService.hasRoomConflict(request.roomNumber(), checkIn, checkOut)) {
                ctx.status(409).json(new ErrorResponse(
                    ErrorCode.ROOM_UNAVAILABLE.name(),
                    "Room is not available for the selected dates"
                ));
                return;
            }

            RatePlan ratePlan;
            try {
                ratePlan = RatePlan.valueOf(request.ratePlan());
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse(
                    ErrorCode.INVALID_REQUEST.name(),
                    "Invalid rate plan"
                ));
                return;
            }

            Reservation reservation = reservationService.createReservation(
                request.roomNumber(), room, request.guestName(), request.guestEmail(),
                request.guestCpf(), checkIn, checkOut, request.guests(), ratePlan
            );

            ctx.status(201).json(toDTO(reservation));

        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse(
                ErrorCode.INVALID_REQUEST.name(),
                "Invalid request body"
            ));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id).orElse(null);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse(
                    ErrorCode.RESERVATION_NOT_FOUND.name(),
                    "Reservation not found"
                ));
                return;
            }

            ctx.json(toDTO(reservation));
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse(
                ErrorCode.RESERVATION_NOT_FOUND.name(),
                "Invalid reservation ID"
            ));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            try {
                Reservation reservation = reservationService.confirmReservation(id);
                ctx.json(toDTO(reservation));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(new ErrorResponse(
                    ErrorCode.RESERVATION_NOT_FOUND.name(),
                    "Reservation not found"
                ));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ErrorResponse(
                    ErrorCode.OPERATION_NOT_ALLOWED.name(),
                    "Cannot confirm reservation in current status"
                ));
            }
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse(
                ErrorCode.RESERVATION_NOT_FOUND.name(),
                "Invalid reservation ID"
            ));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            try {
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(toDTO(reservation));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(new ErrorResponse(
                    ErrorCode.RESERVATION_NOT_FOUND.name(),
                    "Reservation not found"
                ));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ErrorResponse(
                    ErrorCode.OPERATION_NOT_ALLOWED.name(),
                    "Cannot cancel reservation in current status"
                ));
            }
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse(
                ErrorCode.RESERVATION_NOT_FOUND.name(),
                "Invalid reservation ID"
            ));
        }
    }

    public void checkInReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            try {
                Reservation reservation = reservationService.checkInReservation(id);
                ctx.json(toDTO(reservation));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(new ErrorResponse(
                    ErrorCode.RESERVATION_NOT_FOUND.name(),
                    "Reservation not found"
                ));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ErrorResponse(
                    ErrorCode.OPERATION_NOT_ALLOWED.name(),
                    "Cannot check-in reservation in current status"
                ));
            }
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse(
                ErrorCode.RESERVATION_NOT_FOUND.name(),
                "Invalid reservation ID"
            ));
        }
    }

    public void checkOutReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            try {
                Reservation reservation = reservationService.checkOutReservation(id);
                ctx.json(toDocumentDTO(reservation));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(new ErrorResponse(
                    ErrorCode.RESERVATION_NOT_FOUND.name(),
                    "Reservation not found"
                ));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ErrorResponse(
                    ErrorCode.OPERATION_NOT_ALLOWED.name(),
                    "Cannot check-out reservation in current status"
                ));
            }
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse(
                ErrorCode.RESERVATION_NOT_FOUND.name(),
                "Invalid reservation ID"
            ));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id).orElse(null);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse(
                    ErrorCode.RESERVATION_NOT_FOUND.name(),
                    "Reservation not found"
                ));
                return;
            }

            if (reservation.getStatus() == ReservationStatus.PENDING ||
                reservation.getStatus() == ReservationStatus.CANCELLED) {
                ctx.status(409).json(new ErrorResponse(
                    ErrorCode.OPERATION_NOT_ALLOWED.name(),
                    "Document only available for paid reservations"
                ));
                return;
            }

            ctx.json(toDocumentDTO(reservation));
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse(
                ErrorCode.RESERVATION_NOT_FOUND.name(),
                "Invalid reservation ID"
            ));
        }
    }

    private ReservationDTO toDTO(Reservation reservation) {
        return new ReservationDTO(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            reservation.getGuests(),
            reservation.getRatePlan().name(),
            reservation.getNights(),
            reservation.getSubtotal().doubleValue(),
            reservation.getDiscount().doubleValue(),
            reservation.getTotal().doubleValue(),
            reservation.getStatus().name(),
            reservation.getAmountPaid().doubleValue(),
            reservation.getRefund() != null ? reservation.getRefund().doubleValue() : null
        );
    }

    private DocumentDTO toDocumentDTO(Reservation reservation) {
        return new DocumentDTO(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            getRoomTypeName(reservation.getRoomNumber()),
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            reservation.getNights(),
            reservation.getSubtotal().doubleValue(),
            reservation.getDiscount().doubleValue(),
            reservation.getTotal().doubleValue()
        );
    }

    private String getRoomTypeName(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }
}
