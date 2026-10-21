package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.dto.*;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class Application {
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final ReportService reportService;
    private final RoomRepository roomRepository;

    public Application() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        this.roomRepository = roomRepository;
        this.reservationService = new ReservationService(reservationRepository, roomRepository);
        this.notificationService = new NotificationService(messageRepository);
        this.reportService = new ReportService(reservationRepository, roomRepository);
    }

    public Javalin createApp() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper, false));
        });

        app.get("/rooms", ctx -> {
            var rooms = roomRepository.findAll().stream()
                    .map(RoomDto::from)
                    .toList();
            ctx.json(rooms);
        });

        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

                validateRequest(request);

                RatePlan ratePlan = RatePlan.valueOf(request.ratePlan());

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

                ctx.status(201).json(ReservationDto.from(reservation));
            } catch (IllegalArgumentException e) {
                handleBadRequest(ctx, e.getMessage());
            } catch (ReservationService.RoomNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.RoomUnavailableException e) {
                ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
            } catch (ReservationService.CapacityExceededException e) {
                ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
            } catch (ReservationService.InvalidRequestException e) {
                handleBadRequest(ctx, e.getMessage());
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id)
                        .orElseThrow(() -> new ReservationService.ReservationNotFoundException("Reservation not found"));
                ctx.json(ReservationDto.from(reservation));
            } catch (NumberFormatException e) {
                handleBadRequest(ctx, "Invalid reservation ID");
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.confirmReservation(id);

                Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
                String receiptText = ReceiptFormatter.format(reservation, room);
                notificationService.sendConfirmationEmail(reservation, receiptText);

                ctx.json(ReservationDto.from(reservation));
            } catch (NumberFormatException e) {
                handleBadRequest(ctx, "Invalid reservation ID");
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(ReservationDto.from(reservation));
            } catch (NumberFormatException e) {
                handleBadRequest(ctx, "Invalid reservation ID");
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(ReservationDto.from(reservation));
            } catch (NumberFormatException e) {
                handleBadRequest(ctx, "Invalid reservation ID");
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkOut(id);
                Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
                ctx.json(ReceiptDto.from(reservation, room));
            } catch (NumberFormatException e) {
                handleBadRequest(ctx, "Invalid reservation ID");
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (ReservationService.OperationNotAllowedException e) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id)
                        .orElseThrow(() -> new ReservationService.ReservationNotFoundException("Reservation not found"));

                if (!reservation.isPaid()) {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
                    return;
                }

                Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
                ctx.json(ReceiptDto.from(reservation, room));
            } catch (NumberFormatException e) {
                handleBadRequest(ctx, "Invalid reservation ID");
            } catch (ReservationService.ReservationNotFoundException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            }
        });

        app.get("/messages", ctx -> {
            var messages = notificationService.getAllMessages().stream()
                    .map(MessageDto::from)
                    .toList();
            ctx.json(messages);
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateParam = ctx.queryParam("date");
                if (dateParam == null) {
                    handleBadRequest(ctx, "Missing date parameter");
                    return;
                }

                LocalDate date = LocalDate.parse(dateParam);
                String csv = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (DateTimeParseException e) {
                handleBadRequest(ctx, "Invalid date format");
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null) {
                    handleBadRequest(ctx, "Missing month parameter");
                    return;
                }

                YearMonth month = YearMonth.parse(monthParam);
                String csv = reportService.generateRevenueReport(month);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (DateTimeParseException e) {
                handleBadRequest(ctx, "Invalid month format");
            }
        });

        app.get("/reports/tourism-tax", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null) {
                    handleBadRequest(ctx, "Missing month parameter");
                    return;
                }

                YearMonth month = YearMonth.parse(monthParam);
                String csv = reportService.generateTourismTaxReport(month);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (DateTimeParseException e) {
                handleBadRequest(ctx, "Invalid month format");
            }
        });

        app.get("/reports/rate-plans", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null) {
                    handleBadRequest(ctx, "Missing month parameter");
                    return;
                }

                YearMonth month = YearMonth.parse(monthParam);
                String csv = reportService.generateRatePlansReport(month);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (DateTimeParseException e) {
                handleBadRequest(ctx, "Invalid month format");
            }
        });

        return app;
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.guestName() == null ||
            request.guestEmail() == null || request.guestCpf() == null ||
            request.checkIn() == null || request.checkOut() == null ||
            request.ratePlan() == null) {
            throw new ReservationService.InvalidRequestException("Missing required fields");
        }
    }

    private void handleBadRequest(Context ctx, String message) {
        ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", message));
    }

    public static void main(String[] args) {
        int port = getPort();
        Application application = new Application();
        Javalin app = application.createApp();
        app.start(port);
        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor iniciado na porta " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            return Integer.parseInt(portEnv);
        }
        return 7070;
    }
}
