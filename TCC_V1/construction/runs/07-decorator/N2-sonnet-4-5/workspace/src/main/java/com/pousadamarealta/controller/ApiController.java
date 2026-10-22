package com.pousadamarealta.controller;

import com.pousadamarealta.controller.dto.*;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.messaging.MessageService;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class ApiController {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;
    private final MessageService messageService;
    private final ReportService reportService;

    public ApiController(ReservationService reservationService, RoomRepository roomRepository,
                        MessageService messageService, ReportService reportService) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
        this.messageService = messageService;
        this.reportService = reportService;
    }

    public void getRooms(Context ctx) {
        List<RoomDto> rooms = roomRepository.findAll().stream()
            .map(RoomDto::from)
            .toList();
        ctx.json(rooms);
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            LocalDate checkIn = parseDate(request.checkIn());
            LocalDate checkOut = parseDate(request.checkOut());

            if (!checkOut.isAfter(checkIn)) {
                throw new ValidationException("Check-out must be after check-in");
            }

            RatePlan ratePlan = parseRatePlan(request.ratePlan());

            Reservation reservation = reservationService.createReservation(
                request.roomNumber(),
                request.guestName(),
                request.guestEmail(),
                request.guestCpf(),
                checkIn,
                checkOut,
                request.guests(),
                ratePlan
            );

            ctx.status(HttpStatus.CREATED);
            ctx.json(ReservationDto.from(reservation));

        } catch (ValidationException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        } catch (ReservationService.InvalidRequestException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(ReservationDto.from(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationDto.from(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot confirm this reservation"));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationDto.from(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot cancel this reservation"));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationDto.from(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check-in this reservation"));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            Room room = roomRepository.findByNumber(reservation.roomNumber())
                .orElseThrow(() -> new RuntimeException("Room not found"));
            ctx.json(DocumentDto.from(reservation, room));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } catch (ReservationService.OperationNotAllowedException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check-out this reservation"));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.amountPaid().compareTo(java.math.BigDecimal.ZERO) == 0) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Document only available for paid reservations"));
                return;
            }

            Room room = roomRepository.findByNumber(reservation.roomNumber())
                .orElseThrow(() -> new RuntimeException("Room not found"));
            ctx.json(DocumentDto.from(reservation, room));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        }
    }

    public void getMessages(Context ctx) {
        List<MessageDto> messages = messageService.findAll().stream()
            .map(MessageDto::from)
            .toList();
        ctx.json(messages);
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
                return;
            }

            LocalDate date = parseDate(dateStr);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (ValidationException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
                return;
            }

            YearMonth month = parseMonth(monthStr);
            String csv = reportService.generateRevenueReport(month);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (ValidationException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format");
        }
    }

    private YearMonth parseMonth(String monthStr) {
        try {
            return YearMonth.parse(monthStr);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid month format");
        }
    }

    private RatePlan parseRatePlan(String ratePlanStr) {
        try {
            return RatePlan.valueOf(ratePlanStr);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid rate plan");
        }
    }

    private static class ValidationException extends RuntimeException {
        ValidationException(String message) {
            super(message);
        }
    }
}
