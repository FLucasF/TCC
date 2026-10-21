package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamarealta.api.dto.*;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.events.EventPublisher;
import com.pousadamarealta.events.FinanceListener;
import com.pousadamarealta.events.GovernanceListener;
import com.pousadamarealta.events.NotificationService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReceiptService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.ContentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class Application {
    public static void main(String[] args) {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();
        EventPublisher eventPublisher = new EventPublisher();
        ReceiptService receiptService = new ReceiptService(roomRepository);
        ReservationService reservationService = new ReservationService(
            roomRepository, reservationRepository, messageRepository, eventPublisher, receiptService
        );
        ReportService reportService = new ReportService(roomRepository, reservationRepository);

        NotificationService notificationService = new NotificationService(messageRepository);
        eventPublisher.subscribe(new GovernanceListener(notificationService));
        eventPublisher.subscribe(new FinanceListener(notificationService));

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        Javalin app = Javalin.create().start(port);

        app.get("/rooms", ctx -> {
            java.util.List<RoomDto> rooms = new java.util.ArrayList<>();
            for (Room room : roomRepository.findAll()) {
                rooms.add(new RoomDto(room.getNumber(), room.getType().getName(),
                                     room.getCapacity(), room.getNightlyRate()));
            }
            ctx.json(rooms);
        });

        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);
                validateReservationRequest(request);

                LocalDate checkIn = LocalDate.parse(request.checkIn);
                LocalDate checkOut = LocalDate.parse(request.checkOut);

                Reservation reservation = reservationService.createReservation(
                    request.roomNumber, request.guestName, request.guestEmail, request.guestCpf,
                    checkIn, checkOut, request.guests, RatePlan.valueOf(request.ratePlan)
                );
                ctx.status(201).json(ReservationDto.fromReservation(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationRepository.findById(id);
                if (reservation == null) {
                    handleError(ctx, "RESERVATION_NOT_FOUND");
                } else {
                    ctx.json(ReservationDto.fromReservation(reservation));
                }
            } catch (NumberFormatException e) {
                handleError(ctx, "INVALID_REQUEST");
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var body = ctx.bodyAsClass(java.util.Map.class);
                if (!body.containsKey("amountPaid")) {
                    handleError(ctx, "INVALID_REQUEST");
                    return;
                }
                BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());
                Reservation reservation = reservationService.confirmReservation(id, amountPaid);
                ctx.json(ReservationDto.fromReservation(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(ReservationDto.fromReservation(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(ReservationDto.fromReservation(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkOut(id);
                ctx.json(DocumentDto.fromReservation(reservation, roomRepository));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationRepository.findById(id);
                if (reservation == null) {
                    handleError(ctx, "RESERVATION_NOT_FOUND");
                } else if (!reservation.isPaid()) {
                    handleError(ctx, "OPERATION_NOT_ALLOWED");
                } else {
                    ctx.json(DocumentDto.fromReservation(reservation, roomRepository));
                }
            } catch (NumberFormatException e) {
                handleError(ctx, "INVALID_REQUEST");
            }
        });

        app.get("/messages", ctx -> {
            java.util.List<MessageDto> messages = new java.util.ArrayList<>();
            for (Message message : messageRepository.findAll()) {
                messages.add(MessageDto.fromMessage(message));
            }
            ctx.json(messages);
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isEmpty()) {
                    handleError(ctx, "INVALID_REQUEST");
                    return;
                }
                LocalDate date = LocalDate.parse(dateStr);
                String report = reportService.generateOccupancyReport(date);
                ctx.contentType(ContentType.TEXT_PLAIN).result(report);
            } catch (Exception e) {
                handleError(ctx, "INVALID_REQUEST");
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    handleError(ctx, "INVALID_REQUEST");
                    return;
                }
                YearMonth yearMonth = YearMonth.parse(monthStr);
                String report = reportService.generateRevenueReport(yearMonth);
                ctx.contentType(ContentType.TEXT_PLAIN).result(report);
            } catch (Exception e) {
                handleError(ctx, "INVALID_REQUEST");
            }
        });
    }

    private static void validateReservationRequest(CreateReservationRequest request) {
        if (request.roomNumber == 0 || request.guestName == null || request.guestEmail == null ||
            request.guestCpf == null || request.checkIn == null || request.checkOut == null ||
            request.guests == 0 || request.ratePlan == null) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
        try {
            LocalDate.parse(request.checkIn);
            LocalDate.parse(request.checkOut);
            RatePlan.valueOf(request.ratePlan);
        } catch (Exception e) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
    }

    private static void handleError(io.javalin.http.Context ctx, String error) {
        String message = switch (error) {
            case "ROOM_NOT_FOUND" -> "Quarto não encontrado";
            case "RESERVATION_NOT_FOUND" -> "Reserva não encontrada";
            case "ROOM_UNAVAILABLE" -> "Quarto não disponível para as datas solicitadas";
            case "OPERATION_NOT_ALLOWED" -> "Operação não permitida para este status de reserva";
            case "CAPACITY_EXCEEDED" -> "Número de hóspedes excede a capacidade do quarto";
            default -> "Requisição inválida";
        };

        int statusCode = switch (error) {
            case "ROOM_NOT_FOUND", "RESERVATION_NOT_FOUND" -> 404;
            case "ROOM_UNAVAILABLE", "OPERATION_NOT_ALLOWED" -> 409;
            case "CAPACITY_EXCEEDED" -> 422;
            default -> 400;
        };

        ctx.status(statusCode).json(new ErrorDto(error, message));
    }
}
