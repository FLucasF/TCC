package com.pousadamarealta.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.service.MessagingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.web.dto.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class WebApplication {
    private final ReservationService reservationService;
    private final MessagingService messagingService;
    private final ReportService reportService;
    private final com.pousadamarealta.repository.RoomRepository roomRepository;

    public WebApplication(ReservationService reservationService,
                         MessagingService messagingService,
                         ReportService reportService,
                         com.pousadamarealta.repository.RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.messagingService = messagingService;
        this.reportService = reportService;
        this.roomRepository = roomRepository;
    }

    public Javalin createApp() {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper, true));
        });

        app.get("/rooms", ctx -> {
            var rooms = roomRepository.findAll();
            ctx.json(rooms.stream().map(RoomResponse::from).toList());
        });

        app.post("/reservations", ctx -> {
            try {
                var request = ctx.bodyAsClass(CreateReservationRequest.class);
                validateCreateReservation(request);

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

                ctx.status(201).json(ReservationResponse.from(reservation));
            } catch (ReservationService.InvalidRequestException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            } catch (ReservationService.RoomNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.RoomUnavailableException e) {
                ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
            } catch (ReservationService.CapacityExceededException e) {
                ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
            } catch (Exception e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request: " + e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.findById(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.confirmReservation(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.cancelReservation(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.checkIn(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.checkOut(id);
                var document = reservationService.getDocument(id);
                ctx.json(DocumentResponse.from(document));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (IllegalStateException | ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var document = reservationService.getDocument(id);
                ctx.json(DocumentResponse.from(document));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.get("/messages", ctx -> {
            var messages = messagingService.findAll();
            ctx.json(messages.stream().map(MessageResponse::from).toList());
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isBlank()) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing 'date' parameter"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                String csv = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isBlank()) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing 'month' parameter"));
                    return;
                }

                String[] parts = monthStr.split("-");
                if (parts.length != 2) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
                    return;
                }

                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);

                String csv = reportService.generateRevenueReport(year, month);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
            }
        });

        return app;
    }

    private void validateCreateReservation(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.roomNumber().isBlank()) {
            throw new ReservationService.InvalidRequestException("Missing roomNumber");
        }
        if (request.guestName() == null || request.guestName().isBlank()) {
            throw new ReservationService.InvalidRequestException("Missing guestName");
        }
        if (request.guestEmail() == null || request.guestEmail().isBlank()) {
            throw new ReservationService.InvalidRequestException("Missing guestEmail");
        }
        if (request.guestCpf() == null || request.guestCpf().isBlank()) {
            throw new ReservationService.InvalidRequestException("Missing guestCpf");
        }
        if (request.checkIn() == null) {
            throw new ReservationService.InvalidRequestException("Missing checkIn");
        }
        if (request.checkOut() == null) {
            throw new ReservationService.InvalidRequestException("Missing checkOut");
        }
        if (request.ratePlan() == null) {
            throw new ReservationService.InvalidRequestException("Missing ratePlan");
        }
    }
}
