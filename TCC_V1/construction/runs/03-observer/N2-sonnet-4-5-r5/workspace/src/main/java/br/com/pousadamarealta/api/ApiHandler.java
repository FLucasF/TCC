package br.com.pousadamarealta.api;

import br.com.pousadamarealta.api.dto.*;
import br.com.pousadamarealta.domain.RatePlan;
import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.domain.ReservationStatus;
import br.com.pousadamarealta.repository.MessageRepository;
import br.com.pousadamarealta.repository.RoomRepository;
import br.com.pousadamarealta.service.*;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

public final class ApiHandler {
    private final RoomRepository roomRepository;
    private final ReservationService reservationService;
    private final ReportService reportService;
    private final MessageRepository messageRepository;

    public ApiHandler(RoomRepository roomRepository,
                     ReservationService reservationService,
                     ReportService reportService,
                     MessageRepository messageRepository) {
        this.roomRepository = roomRepository;
        this.reservationService = reservationService;
        this.reportService = reportService;
        this.messageRepository = messageRepository;
    }

    public void getRooms(Context ctx) {
        var rooms = roomRepository.findAll().stream()
            .map(RoomDto::from)
            .collect(Collectors.toList());
        ctx.json(rooms);
    }

    public void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            if (request.checkOut().isBefore(request.checkIn()) ||
                request.checkOut().equals(request.checkIn())) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse("INVALID_REQUEST", "Check-out must be after check-in"));
                return;
            }

            if (request.guests() < 1) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse("INVALID_REQUEST", "At least one guest is required"));
                return;
            }

            RatePlan ratePlan;
            try {
                ratePlan = RatePlan.valueOf(request.ratePlan());
            } catch (IllegalArgumentException e) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
                return;
            }

            Reservation reservation = reservationService.createReservation(
                request.roomNumber(),
                request.guestName(),
                request.guestEmail(),
                request.guestCpf(),
                request.checkIn(),
                request.checkOut(),
                request.guests(),
                ratePlan
            );

            ctx.status(HttpStatus.CREATED).json(ReservationDto.from(reservation));

        } catch (RoomNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
        } catch (RoomUnavailableException e) {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("capacity")) {
                ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
                   .json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
            } else {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(ReservationDto.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationDto.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationDto.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationDto.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            ctx.json(DocumentDto.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.status() == ReservationStatus.PENDING ||
                reservation.status() == ReservationStatus.CANCELLED) {
                ctx.status(HttpStatus.CONFLICT)
                   .json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
                return;
            }

            ctx.json(DocumentDto.from(reservation));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        } catch (ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    public void getMessages(Context ctx) {
        var messages = messageRepository.findAll().stream()
            .map(MessageDto::from)
            .collect(Collectors.toList());
        ctx.json(messages);
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
