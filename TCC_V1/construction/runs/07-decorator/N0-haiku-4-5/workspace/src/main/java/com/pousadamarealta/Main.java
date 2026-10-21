package com.pousadamarealta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamarealta.dto.*;
import com.pousadamarealta.model.*;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.ContentType;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        boolean testMode = "true".equals(System.getenv("HOTEL_TEST_MODE"));
        int port = 7070;
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                // use default
            }
        }

        MessageService messageService = new MessageService(testMode);
        ReservationService reservationService = new ReservationService(messageService);
        ReportService reportService = new ReportService(reservationService);
        ObjectMapper objectMapper = new ObjectMapper();

        Javalin app = Javalin.create().start(port);

        // GET /rooms
        app.get("/rooms", ctx -> {
            var rooms = reservationService.getRooms().stream()
                    .map(RoomResponse::from)
                    .collect(Collectors.toList());
            ctx.json(rooms);
        });

        // POST /reservations
        app.post("/reservations", ctx -> {
            try {
                CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);

                if (request.roomNumber == 0 || request.guestName == null || request.guestEmail == null ||
                    request.guestCpf == null || request.checkIn == null || request.checkOut == null ||
                    request.ratePlan == null) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                    return;
                }

                try {
                    LocalDate checkInDate = LocalDate.parse(request.checkIn);
                    LocalDate checkOutDate = LocalDate.parse(request.checkOut);

                    RatePlan ratePlan;
                    try {
                        ratePlan = RatePlan.valueOf(request.ratePlan);
                    } catch (IllegalArgumentException e) {
                        ctx.status(400);
                        ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
                        return;
                    }

                    Reservation reservation = reservationService.createReservation(
                            request.roomNumber,
                            request.guestName,
                            request.guestEmail,
                            request.guestCpf,
                            checkInDate,
                            checkOutDate,
                            request.guests,
                            ratePlan
                    );

                    ctx.status(201);
                    ctx.json(ReservationResponse.from(reservation));
                } catch (Exception e) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
                }
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request body"));
            }
        });

        // GET /reservations/{id}
        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else {
                    ctx.json(ReservationResponse.from(reservation));
                }
            } catch (NumberFormatException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            }
        });

        // POST /reservations/{id}/confirmation
        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                try {
                    Reservation confirmed = reservationService.confirmReservation(id, reservation.getTotal());
                    ctx.json(ReservationResponse.from(confirmed));
                } catch (IllegalArgumentException e) {
                    handleReservationError(ctx, e.getMessage());
                }
            } catch (NumberFormatException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            }
        });

        // POST /reservations/{id}/check-in
        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                try {
                    Reservation checkedIn = reservationService.checkIn(id);
                    ctx.json(ReservationResponse.from(checkedIn));
                } catch (IllegalArgumentException e) {
                    handleReservationError(ctx, e.getMessage());
                }
            } catch (NumberFormatException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            }
        });

        // POST /reservations/{id}/check-out
        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                try {
                    String comprovante = reservationService.checkOut(id);
                    ctx.header("Content-Type", "text/plain; charset=utf-8");
                    ctx.result(comprovante);
                } catch (IllegalArgumentException e) {
                    handleReservationError(ctx, e.getMessage());
                }
            } catch (NumberFormatException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            }
        });

        // GET /reservations/{id}/document
        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                try {
                    String comprovante = reservationService.getDocument(id);
                    ctx.header("Content-Type", "text/plain; charset=utf-8");
                    ctx.result(comprovante);
                } catch (IllegalArgumentException e) {
                    handleReservationError(ctx, e.getMessage());
                }
            } catch (NumberFormatException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            }
        });

        // POST /reservations/{id}/cancellation
        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                try {
                    Reservation cancelled = reservationService.cancelReservation(id);
                    ctx.json(ReservationResponse.from(cancelled));
                } catch (IllegalArgumentException e) {
                    handleReservationError(ctx, e.getMessage());
                }
            } catch (NumberFormatException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
            }
        });

        // GET /messages
        app.get("/messages", ctx -> {
            var messages = messageService.getMessages().stream()
                    .map(MessageResponse::from)
                    .collect(Collectors.toList());
            ctx.json(messages);
        });

        // GET /reports/occupancy
        app.get("/reports/occupancy", ctx -> {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            try {
                LocalDate date = LocalDate.parse(dateStr);
                String report = reportService.generateOccupancyReport(date);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
            }
        });

        // GET /reports/revenue
        app.get("/reports/revenue", ctx -> {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            try {
                YearMonth month = YearMonth.parse(monthStr);
                String report = reportService.generateRevenueReport(month);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
            }
        });
    }

    private static void handleReservationError(io.javalin.http.Context ctx, String error) {
        switch (error) {
            case "ROOM_NOT_FOUND" -> {
                ctx.status(404);
                ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
            }
            case "ROOM_UNAVAILABLE" -> {
                ctx.status(409);
                ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for selected dates"));
            }
            case "CAPACITY_EXCEEDED" -> {
                ctx.status(422);
                ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
            }
            case "OPERATION_NOT_ALLOWED" -> {
                ctx.status(409);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "This operation is not allowed for the current reservation status"));
            }
            case "INVALID_REQUEST" -> {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", error));
            }
            default -> {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", error));
            }
        }
    }
}
