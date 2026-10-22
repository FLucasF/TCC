package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.*;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.infrastructure.MessageRepository;
import com.pousadamarealta.infrastructure.RoomRepository;
import com.pousadamarealta.service.NotificationService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

public class ApiHandlers {
    private final RoomRepository roomRepository;
    private final ReservationService reservationService;
    private final MessageRepository messageRepository;
    private final NotificationService notificationService;
    private final ReportService reportService;

    public ApiHandlers(RoomRepository roomRepository,
                      ReservationService reservationService,
                      MessageRepository messageRepository,
                      NotificationService notificationService,
                      ReportService reportService) {
        this.roomRepository = roomRepository;
        this.reservationService = reservationService;
        this.messageRepository = messageRepository;
        this.notificationService = notificationService;
        this.reportService = reportService;
    }

    public void getRooms(Context ctx) {
        var rooms = roomRepository.findAll().stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());
        ctx.json(rooms);
    }

    public void createReservation(Context ctx) {
        try {
            var request = ctx.bodyAsClass(CreateReservationRequest.class);

            validateRequest(request);

            var reservation = reservationService.createReservation(
                    request.roomNumber(),
                    request.guestName(),
                    request.guestEmail(),
                    request.guestCpf(),
                    request.checkIn(),
                    request.checkOut(),
                    request.guests(),
                    request.ratePlan()
            );

            ctx.status(HttpStatus.CREATED).json(ReservationResponse.from(reservation));

        } catch (IllegalArgumentException e) {
            handleBadRequest(ctx, e.getMessage());
        } catch (ReservationService.RoomNotFoundException e) {
            handleError(ctx, HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", e.getMessage());
        } catch (ReservationService.RoomUnavailableException e) {
            handleError(ctx, HttpStatus.CONFLICT, "ROOM_UNAVAILABLE", e.getMessage());
        } catch (ReservationService.CapacityExceededException e) {
            handleError(ctx, HttpStatus.UNPROCESSABLE_CONTENT, "CAPACITY_EXCEEDED", e.getMessage());
        } catch (ReservationService.InvalidRequestException e) {
            handleBadRequest(ctx, e.getMessage());
        }
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.guestName() == null ||
            request.guestEmail() == null || request.guestCpf() == null ||
            request.checkIn() == null || request.checkOut() == null ||
            request.guests() == null || request.ratePlan() == null) {
            throw new ReservationService.InvalidRequestException("Campos obrigatórios faltando");
        }
    }

    public void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.findReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            handleBadRequest(ctx, "ID inválido");
        } catch (ReservationService.ReservationNotFoundException e) {
            handleError(ctx, HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", e.getMessage());
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            handleBadRequest(ctx, "ID inválido");
        } catch (ReservationService.ReservationNotFoundException e) {
            handleError(ctx, HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (IllegalStateException e) {
            handleError(ctx, HttpStatus.CONFLICT, "OPERATION_NOT_ALLOWED", e.getMessage());
        }
    }

    public void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            handleBadRequest(ctx, "ID inválido");
        } catch (ReservationService.ReservationNotFoundException e) {
            handleError(ctx, HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (IllegalStateException e) {
            handleError(ctx, HttpStatus.CONFLICT, "OPERATION_NOT_ALLOWED", e.getMessage());
        }
    }

    public void checkInReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (NumberFormatException e) {
            handleBadRequest(ctx, "ID inválido");
        } catch (ReservationService.ReservationNotFoundException e) {
            handleError(ctx, HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (IllegalStateException e) {
            handleError(ctx, HttpStatus.CONFLICT, "OPERATION_NOT_ALLOWED", e.getMessage());
        }
    }

    public void checkOutReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            var reservation = reservationService.checkOut(id);
            ctx.json(DocumentResponse.from(reservation));
        } catch (NumberFormatException e) {
            handleBadRequest(ctx, "ID inválido");
        } catch (ReservationService.ReservationNotFoundException e) {
            handleError(ctx, HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", e.getMessage());
        } catch (IllegalStateException e) {
            handleError(ctx, HttpStatus.CONFLICT, "OPERATION_NOT_ALLOWED", e.getMessage());
        }
    }

    public void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.findReservation(id);

            if (!reservation.isPaid()) {
                handleError(ctx, HttpStatus.CONFLICT, "OPERATION_NOT_ALLOWED",
                          "Comprovante só existe para reserva paga");
                return;
            }

            ctx.json(DocumentResponse.from(reservation));
        } catch (NumberFormatException e) {
            handleBadRequest(ctx, "ID inválido");
        } catch (ReservationService.ReservationNotFoundException e) {
            handleError(ctx, HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", e.getMessage());
        }
    }

    public void getMessages(Context ctx) {
        var messages = messageRepository.findAll().stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
        ctx.json(messages);
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                handleBadRequest(ctx, "Parâmetro 'date' é obrigatório");
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);

        } catch (DateTimeParseException e) {
            handleBadRequest(ctx, "Formato de data inválido");
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                handleBadRequest(ctx, "Parâmetro 'month' é obrigatório");
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);

        } catch (DateTimeParseException e) {
            handleBadRequest(ctx, "Formato de mês inválido");
        }
    }

    public void getTourismTaxReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                handleBadRequest(ctx, "Parâmetro 'month' é obrigatório");
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateTourismTaxReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);

        } catch (DateTimeParseException e) {
            handleBadRequest(ctx, "Formato de mês inválido");
        }
    }

    public void getRatePlansReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                handleBadRequest(ctx, "Parâmetro 'month' é obrigatório");
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateRatePlansReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);

        } catch (DateTimeParseException e) {
            handleBadRequest(ctx, "Formato de mês inválido");
        }
    }

    private void handleBadRequest(Context ctx, String message) {
        handleError(ctx, HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }

    private void handleError(Context ctx, HttpStatus status, String code, String message) {
        ctx.status(status).json(new ErrorResponse(code, message));
    }
}
