package com.pousadamarealta.api;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public final class ApiServer {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;
    private final MessageService messageService;
    private final ReportService reportService;

    public ApiServer(
        ReservationService reservationService,
        RoomRepository roomRepository,
        MessageService messageService,
        ReportService reportService
    ) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
        this.messageService = messageService;
        this.reportService = reportService;
    }

    public Javalin start(int port) {
        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson()
                .updateMapper(mapper -> {
                    mapper.findAndRegisterModules();
                    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                })
            );
        });

        app.get("/rooms", this::getRooms);
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkIn);
        app.post("/reservations/{id}/check-out", this::checkOut);
        app.get("/reservations/{id}/document", this::getDocument);
        app.get("/messages", this::getMessages);
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            if (ctx.status() == HttpStatus.OK) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(new ApiError("INTERNAL_ERROR", "Internal server error"));
            }
        });

        return app.start(port);
    }

    private void getRooms(Context ctx) {
        ctx.json(roomRepository.findAll().stream()
            .map(RoomResponse::from)
            .toList());
    }

    private void createReservation(Context ctx) {
        try {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

            if (request.checkOut() == null || request.checkIn() == null ||
                !request.checkOut().isAfter(request.checkIn())) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ApiError("INVALID_REQUEST", "Check-out must be after check-in"));
                return;
            }

            if (request.guests() < 1) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ApiError("INVALID_REQUEST", "Guests must be at least 1"));
                return;
            }

            RatePlan ratePlan;
            try {
                ratePlan = RatePlan.valueOf(request.ratePlan());
            } catch (IllegalArgumentException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ApiError("INVALID_REQUEST", "Invalid rate plan"));
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

            ctx.status(HttpStatus.CREATED);
            ctx.json(ReservationResponse.from(reservation));

        } catch (ReservationService.RoomNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ApiError("ROOM_NOT_FOUND", "Room not found"));
        } catch (ReservationService.CapacityExceededException e) {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            ctx.json(new ApiError("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
        } catch (ReservationService.RoomUnavailableException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ApiError("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ApiError("INVALID_REQUEST", "Invalid request: " + e.getMessage()));
        }
    }

    private void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id)
                .orElseThrow(() -> new ReservationService.ReservationNotFoundException());
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ApiError("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    private void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.confirmReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ApiError("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ApiError("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    private void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ApiError("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ApiError("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    private void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(ReservationResponse.from(reservation));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ApiError("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ApiError("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    private void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            Room room = roomRepository.findByNumber(reservation.roomNumber()).orElseThrow();
            ctx.json(DocumentResponse.from(reservation, room.type()));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ApiError("RESERVATION_NOT_FOUND", "Reservation not found"));
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ApiError("OPERATION_NOT_ALLOWED", e.getMessage()));
        }
    }

    private void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id)
                .orElseThrow(() -> new ReservationService.ReservationNotFoundException());

            if (reservation.amountPaid().compareTo(java.math.BigDecimal.ZERO) == 0) {
                ctx.status(HttpStatus.CONFLICT);
                ctx.json(new ApiError("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
                return;
            }

            Room room = roomRepository.findByNumber(reservation.roomNumber()).orElseThrow();
            ctx.json(DocumentResponse.from(reservation, room.type()));
        } catch (ReservationService.ReservationNotFoundException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ApiError("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    private void getMessages(Context ctx) {
        ctx.json(messageService.listAll().stream()
            .map(MessageResponse::from)
            .toList());
    }

    private void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ApiError("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType(ContentType.TEXT_CSV + "; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ApiError("INVALID_REQUEST", "Invalid date format"));
        }
    }

    private void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ApiError("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(month);

            ctx.contentType(ContentType.TEXT_CSV + "; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ApiError("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
