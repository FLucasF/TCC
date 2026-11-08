package com.pousada;

import com.google.gson.Gson;
import com.pousada.api.dto.MessageResponse;
import com.pousada.api.dto.ReceiptResponse;
import com.pousada.api.dto.ReservationRequest;
import com.pousada.api.dto.ReservationResponse;
import com.pousada.api.dto.RoomResponse;
import com.pousada.infra.Database;
import com.pousada.infra.MessageRepository;
import com.pousada.infra.ReservationRepository;
import com.pousada.infra.RoomRepository;
import com.pousada.service.ApiException;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class Application {
    private static final Gson gson = new Gson();
    private static Javalin app;

    public static void main(String[] args) {
        Database.initializeSchema();

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();
        ReservationService reservationService = new ReservationService(roomRepository, reservationRepository, messageRepository);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        app = Javalin.create().start(port);

        setupRoutes(app, reservationService, reportService, roomRepository, reservationRepository, messageRepository);

        System.out.println("Pousada Maré Alta started on port " + port);
    }

    private static void setupRoutes(Javalin app, ReservationService reservationService, ReportService reportService,
                                     RoomRepository roomRepository, ReservationRepository reservationRepository,
                                     MessageRepository messageRepository) {
        app.get("/rooms", ctx -> {
            try {
                var rooms = roomRepository.findAll();
                ctx.json(rooms.stream().map(RoomResponse::from).toList());
            } catch (Exception e) {
                handleError(ctx, "INTERNAL_ERROR", e.getMessage(), 500);
            }
        });

        app.post("/reservations", ctx -> {
            try {
                ReservationRequest req = ctx.bodyAsClass(ReservationRequest.class);

                if (req.roomNumber == 0 || req.guestName == null || req.guestEmail == null ||
                    req.guestCpf == null || req.checkIn == null || req.checkOut == null ||
                    req.ratePlan == null) {
                    handleError(ctx, "INVALID_REQUEST", "Campo obrigatório ausente", 400);
                    return;
                }

                try {
                    LocalDate checkIn = LocalDate.parse(req.checkIn);
                    LocalDate checkOut = LocalDate.parse(req.checkOut);

                    var reservation = reservationService.createReservation(
                        req.roomNumber,
                        req.guestName,
                        req.guestEmail,
                        req.guestCpf,
                        checkIn,
                        checkOut,
                        req.guests,
                        req.ratePlan,
                        req.companyCode
                    );

                    ctx.status(201).json(ReservationResponse.from(reservation));
                } catch (DateTimeParseException e) {
                    handleError(ctx, "INVALID_REQUEST", "Formato de data inválido", 400);
                }
            } catch (ApiException e) {
                handleError(ctx, e.getCode(), e.getMessage(), e.getHttpStatus());
            } catch (Exception e) {
                handleError(ctx, "INTERNAL_ERROR", e.getMessage(), 500);
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.getReservation(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (ApiException e) {
                handleError(ctx, e.getCode(), e.getMessage(), e.getHttpStatus());
            } catch (Exception e) {
                handleError(ctx, "INTERNAL_ERROR", e.getMessage(), 500);
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                BigDecimal amountPaid = new BigDecimal("0");

                try {
                    String body = ctx.body();
                    if (body != null && !body.isEmpty()) {
                        var json = gson.fromJson(body, java.util.Map.class);
                        if (json.containsKey("amountPaid")) {
                            amountPaid = new BigDecimal(json.get("amountPaid").toString());
                        }
                    }
                } catch (Exception ignored) {}

                var reservation = reservationService.confirmReservation(id, amountPaid);
                ctx.json(ReservationResponse.from(reservation));
            } catch (ApiException e) {
                handleError(ctx, e.getCode(), e.getMessage(), e.getHttpStatus());
            } catch (Exception e) {
                handleError(ctx, "INTERNAL_ERROR", e.getMessage(), 500);
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.checkIn(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (ApiException e) {
                handleError(ctx, e.getCode(), e.getMessage(), e.getHttpStatus());
            } catch (Exception e) {
                handleError(ctx, "INTERNAL_ERROR", e.getMessage(), 500);
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.checkOut(id);
                var room = new RoomRepository().findByNumber(reservation.getRoomNumber());
                ctx.json(ReceiptResponse.from(reservation, room));
            } catch (ApiException e) {
                handleError(ctx, e.getCode(), e.getMessage(), e.getHttpStatus());
            } catch (Exception e) {
                handleError(ctx, "INTERNAL_ERROR", e.getMessage(), 500);
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.cancelReservation(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (ApiException e) {
                handleError(ctx, e.getCode(), e.getMessage(), e.getHttpStatus());
            } catch (Exception e) {
                handleError(ctx, "INTERNAL_ERROR", e.getMessage(), 500);
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.getReservation(id);

                if (reservation.getAmountPaid().signum() == 0) {
                    handleError(ctx, "OPERATION_NOT_ALLOWED", "Documento disponível apenas para reservas pagas", 409);
                    return;
                }

                var room = new RoomRepository().findByNumber(reservation.getRoomNumber());
                ctx.json(ReceiptResponse.from(reservation, room));
            } catch (ApiException e) {
                handleError(ctx, e.getCode(), e.getMessage(), e.getHttpStatus());
            } catch (Exception e) {
                handleError(ctx, "INTERNAL_ERROR", e.getMessage(), 500);
            }
        });

        app.get("/messages", ctx -> {
            try {
                var messages = messageRepository.findAll();
                ctx.json(messages.stream().map(MessageResponse::from).toList());
            } catch (Exception e) {
                handleError(ctx, "INTERNAL_ERROR", e.getMessage(), 500);
            }
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null) {
                    handleError(ctx, "INVALID_REQUEST", "Parâmetro 'date' obrigatório", 400);
                    return;
                }

                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    String report = reportService.generateOccupancyReport(date);
                    ctx.contentType("text/csv; charset=utf-8").result(report);
                } catch (DateTimeParseException e) {
                    handleError(ctx, "INVALID_REQUEST", "Formato de data inválido", 400);
                }
            } catch (Exception e) {
                handleError(ctx, "INTERNAL_ERROR", e.getMessage(), 500);
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    handleError(ctx, "INVALID_REQUEST", "Parâmetro 'month' obrigatório", 400);
                    return;
                }

                try {
                    YearMonth month = YearMonth.parse(monthStr);
                    String report = reportService.generateRevenueReport(month);
                    ctx.contentType("text/csv; charset=utf-8").result(report);
                } catch (DateTimeParseException e) {
                    handleError(ctx, "INVALID_REQUEST", "Formato de mês inválido", 400);
                }
            } catch (Exception e) {
                handleError(ctx, "INTERNAL_ERROR", e.getMessage(), 500);
            }
        });
    }

    private static void handleError(io.javalin.http.Context ctx, String code, String message, int status) {
        ctx.status(status).json(new java.util.HashMap<String, Object>() {{
            put("error", code);
            put("message", message);
        }});
    }
}
