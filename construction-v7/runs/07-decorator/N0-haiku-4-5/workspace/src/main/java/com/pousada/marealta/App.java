package com.pousada.marealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pousada.marealta.api.converter.*;
import com.pousada.marealta.api.dto.*;
import com.pousada.marealta.model.*;
import com.pousada.marealta.service.*;
import com.pousada.marealta.util.DateUtil;
import com.pousada.marealta.util.DocumentFormatter;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.json.JsonMapper;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class App {
    private final Javalin app;
    private final RoomManager roomManager;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ReportService reportService;
    private final Gson gson;

    public App() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

        this.roomManager = new RoomManager();
        this.reservationService = new ReservationService(roomManager);

        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        this.messageService = new MessageService(testMode);
        this.reportService = new ReportService(reservationService, roomManager);

        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        mapper.registerModule(javaTimeModule);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        this.app = Javalin.create(config -> {
            config.jsonMapper(new JsonMapper() {
                @Override
                public <T> T fromJsonString(String json, Type targetType) {
                    try {
                        return mapper.readValue(json, mapper.constructType(targetType));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public String toJsonString(Object obj, Type type) {
                    try {
                        return mapper.writeValueAsString(obj);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        });
        setupRoutes();
    }

    private void setupRoutes() {
        app.get("/rooms", ctx -> {
            var rooms = roomManager.getAllRooms().stream()
                .map(RoomConverter::toDto)
                .toList();
            ctx.json(rooms);
        });

        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);

                RatePlan ratePlan = RatePlan.valueOf(req.ratePlan());
                Reservation reservation = reservationService.createReservation(
                    req.roomNumber(),
                    req.guestName(),
                    req.guestEmail(),
                    req.guestCpf(),
                    req.checkIn(),
                    req.checkOut(),
                    req.guests(),
                    ratePlan
                );

                ctx.status(201);
                ctx.json(ReservationConverter.toDto(reservation));
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e.code, e.getMessage());
            } catch (IllegalArgumentException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Plano de tarifa inválido"));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id)
                    .orElseThrow(() -> new ReservationService.ValidationException("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                ctx.json(ReservationConverter.toDto(reservation));
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e.code, e.getMessage());
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                ConfirmationRequest req = ctx.bodyAsClass(ConfirmationRequest.class);

                reservationService.confirmReservation(id, req.amountPaid());

                Reservation reservation = reservationService.getReservation(id).orElseThrow();

                var document = DocumentConverter.toDocument(reservation, roomManager);
                String documentText = DocumentFormatter.formatDocument(document);
                String emailBody = formatConfirmationEmail(reservation.getGuestName(), documentText);

                messageService.sendMessage(
                    "EMAIL",
                    reservation.getGuestEmail(),
                    "Reserva confirmada - Pousada Maré Alta",
                    emailBody
                );

                ctx.json(ReservationConverter.toDto(reservation));
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e.code, e.getMessage());
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                reservationService.cancelReservation(id);

                Reservation reservation = reservationService.getReservation(id).orElseThrow();
                ctx.json(ReservationConverter.toDto(reservation));
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e.code, e.getMessage());
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                reservationService.checkin(id);

                Reservation reservation = reservationService.getReservation(id).orElseThrow();
                ctx.json(ReservationConverter.toDto(reservation));
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e.code, e.getMessage());
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                reservationService.checkout(id);

                Reservation reservation = reservationService.getReservation(id).orElseThrow();
                var document = DocumentConverter.toDocument(reservation, roomManager);
                DocumentDto documentDto = DocumentConverter.toDto(document);

                ctx.json(documentDto);
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e.code, e.getMessage());
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id)
                    .orElseThrow(() -> new ReservationService.ValidationException("RESERVATION_NOT_FOUND", "Reserva não encontrada"));

                if (reservation.getStatus() == ReservationStatus.PENDING) {
                    throw new ReservationService.ValidationException("OPERATION_NOT_ALLOWED", "Documento só disponível para reservas pagas");
                }

                var document = DocumentConverter.toDocument(reservation, roomManager);
                DocumentDto documentDto = DocumentConverter.toDto(document);

                ctx.json(documentDto);
            } catch (ReservationService.ValidationException e) {
                handleValidationError(ctx, e.code, e.getMessage());
            }
        });

        app.get("/messages", ctx -> {
            var messages = messageService.getAllMessages().stream()
                .map(MessageConverter::toDto)
                .toList();
            ctx.json(messages);
        });

        app.get("/reports/occupancy", ctx -> {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'date' é obrigatório"));
                return;
            }

            try {
                LocalDate date = LocalDate.parse(dateStr);
                String report = reportService.generateOccupancyReport(date);
                ctx.contentType(ContentType.TEXT_PLAIN);
                ctx.header("Content-Type", "text/csv;charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Data em formato inválido"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'month' é obrigatório"));
                return;
            }

            try {
                YearMonth yearMonth = YearMonth.parse(monthStr);
                String report = reportService.generateRevenueReport(yearMonth);
                ctx.contentType(ContentType.TEXT_PLAIN);
                ctx.header("Content-Type", "text/csv;charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Mês em formato inválido"));
            }
        });
    }

    private void handleValidationError(io.javalin.http.Context ctx, String code, String message) {
        ctx.status(getStatusCode(code));
        ctx.json(new ErrorResponse(code, message));
    }

    private int getStatusCode(String code) {
        return switch (code) {
            case "ROOM_NOT_FOUND", "RESERVATION_NOT_FOUND" -> 404;
            case "ROOM_UNAVAILABLE", "OPERATION_NOT_ALLOWED" -> 409;
            case "CAPACITY_EXCEEDED" -> 422;
            default -> 400;
        };
    }

    private String formatConfirmationEmail(String guestName, String documentText) {
        return "Olá, " + guestName + "!\n\n" +
               "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
               documentText + "\n" +
               "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
               "Até breve!";
    }

    public void start() {
        String portStr = System.getenv("PORT");
        int port = (portStr != null) ? Integer.parseInt(portStr) : 7070;
        app.start(port);
    }

    public Javalin getApp() {
        return app;
    }

    public static void main(String[] args) {
        App app = new App();
        app.start();
    }
}
