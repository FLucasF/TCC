package com.pousada.mare_alta.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.mare_alta.http.request.CreateReservationRequest;
import com.pousada.mare_alta.http.response.DocumentResponse;
import com.pousada.mare_alta.http.response.ErrorResponse;
import com.pousada.mare_alta.http.response.MessageResponse;
import com.pousada.mare_alta.http.response.ReservationResponse;
import com.pousada.mare_alta.http.response.RoomResponse;
import com.pousada.mare_alta.model.RatePlan;
import com.pousada.mare_alta.service.ReportService;
import com.pousada.mare_alta.service.ReservationService;
import com.pousada.mare_alta.repository.MessageRepository;
import com.pousada.mare_alta.repository.ReservationRepository;
import com.pousada.mare_alta.repository.RoomRepository;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AppRoutes {
    private final ReservationService reservationService;
    private final ReportService reportService;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public AppRoutes(
            ReservationService reservationService,
            ReportService reportService,
            RoomRepository roomRepository,
            MessageRepository messageRepository) {
        this.reservationService = reservationService;
        this.reportService = reportService;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.objectMapper = new ObjectMapper();
    }

    public void setupRoutes(Javalin app) {
        // GET /rooms
        app.get("/rooms", ctx -> {
            try {
                var rooms = roomRepository.findAll();
                ctx.json(rooms.stream().map(RoomResponse::fromModel).toList());
            } catch (Exception e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        // POST /reservations
        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);
                validateCreateReservationRequest(req);

                LocalDate checkIn = LocalDate.parse(req.checkIn);
                LocalDate checkOut = LocalDate.parse(req.checkOut);
                RatePlan ratePlan = RatePlan.fromValue(req.ratePlan);

                var reservation = reservationService.createReservation(
                        req.roomNumber, req.guestName, req.guestEmail,
                        req.guestCpf, req.companyCnpj, req.companyName,
                        req.guestPassport, req.guestCountry,
                        checkIn, checkOut, req.guests, ratePlan);

                ctx.status(201).json(ReservationResponse.fromModel(reservation));
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("Room not found")) {
                    ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
                } else if (e.getMessage().contains("room capacity")) {
                    ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
                } else if (e.getMessage().contains("not available")) {
                    ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
                } else if (e.getMessage().contains("identification") ||
                           e.getMessage().contains("Check-out") ||
                           e.getMessage().contains("guest")) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
                } else {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
                }
            } catch (Exception e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        // GET /reservations/{id}
        app.get("/reservations/:id", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.getReservation(id);
                ctx.json(ReservationResponse.fromModel(reservation));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        // POST /reservations/{id}/confirmation
        app.post("/reservations/:id/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var body = ctx.bodyAsClass(java.util.Map.class);
                BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());

                var reservation = reservationService.confirmReservation(id, amountPaid);
                ctx.json(ReservationResponse.fromModel(reservation));
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
                } else {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
                }
            } catch (Exception e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        // POST /reservations/{id}/cancellation
        app.post("/reservations/:id/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.cancelReservation(id);
                ctx.json(ReservationResponse.fromModel(reservation));
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
                } else {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
                }
            } catch (Exception e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        // POST /reservations/{id}/check-in
        app.post("/reservations/:id/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = reservationService.checkIn(id);
                ctx.json(ReservationResponse.fromModel(reservation));
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
                } else {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
                }
            } catch (Exception e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        // POST /reservations/{id}/check-out
        app.post("/reservations/:id/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var document = reservationService.checkOut(id);
                ctx.json(DocumentResponse.fromModel(document));
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
                } else {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
                }
            } catch (Exception e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        // GET /reservations/{id}/document
        app.get("/reservations/:id/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var document = reservationService.getDocument(id);
                ctx.json(DocumentResponse.fromModel(document));
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
                } else {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
                }
            } catch (Exception e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        // GET /messages
        app.get("/messages", ctx -> {
            try {
                var messages = messageRepository.findAll();
                ctx.json(messages.stream().map(MessageResponse::fromModel).toList());
            } catch (Exception e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        // GET /reports/occupancy
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "date parameter is required"));
                    return;
                }
                LocalDate date = LocalDate.parse(dateStr);
                String csv = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (Exception e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        // GET /reports/revenue
        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "month parameter is required"));
                    return;
                }
                String csv = reportService.generateRevenueReport(monthStr);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (Exception e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });
    }

    private void validateCreateReservationRequest(CreateReservationRequest req) throws Exception {
        if (req.guestName == null || req.guestName.isBlank() ||
            req.guestEmail == null || req.guestEmail.isBlank() ||
            req.checkIn == null || req.checkIn.isBlank() ||
            req.checkOut == null || req.checkOut.isBlank() ||
            req.ratePlan == null || req.ratePlan.isBlank()) {
            throw new IllegalArgumentException("Missing required fields");
        }

        try {
            LocalDate.parse(req.checkIn);
            LocalDate.parse(req.checkOut);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format");
        }

        try {
            RatePlan.fromValue(req.ratePlan);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid rate plan");
        }
    }
}
