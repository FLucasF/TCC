package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamarealta.api.*;
import com.pousadamarealta.database.Database;
import com.pousadamarealta.database.MessageRepository;
import com.pousadamarealta.database.ReservationRepository;
import com.pousadamarealta.database.RoomRepository;
import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.models.Room;
import com.pousadamarealta.rateplans.RatePlan;
import com.pousadamarealta.services.ReceiptService;
import com.pousadamarealta.services.ReportService;
import com.pousadamarealta.services.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class Main {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) throws SQLException {
        // Initialize database
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("Missing required environment variables: DB_URL, DB_USER, DB_PASSWORD");
            System.exit(1);
        }

        Database.initialize(dbUrl, dbUser, dbPassword);
        RoomRepository.initializeRooms();

        // Initialize server
        String portEnv = System.getenv("PORT");
        int port = portEnv != null ? Integer.parseInt(portEnv) : 7070;

        Javalin app = Javalin.create();

        // GET /rooms
        app.get("/rooms", ctx -> {
            try {
                var rooms = RoomRepository.findAll();
                var response = rooms.stream()
                    .map(RoomResponse::new)
                    .collect(Collectors.toList());
                ctx.json(response);
            } catch (SQLException e) {
                ctx.status(500).json(new ApiError("DATABASE_ERROR", e.getMessage()));
            }
        });

        // POST /reservations
        app.post("/reservations", ctx -> {
            try {
                ReservationRequest request = ctx.bodyAsClass(ReservationRequest.class);

                if (request.roomNumber == 0 || request.guestName == null || request.guestEmail == null ||
                    request.guestCpf == null || request.checkIn == null || request.checkOut == null ||
                    request.guests == 0 || request.ratePlan == null) {
                    ctx.status(400).json(new ApiError("INVALID_REQUEST", "Missing required fields"));
                    return;
                }

                if (request.guests < 1) {
                    ctx.status(400).json(new ApiError("INVALID_REQUEST", "Guest count must be at least 1"));
                    return;
                }

                // Parse dates
                LocalDate checkIn, checkOut;
                try {
                    checkIn = LocalDate.parse(request.checkIn, DATE_FORMAT);
                    checkOut = LocalDate.parse(request.checkOut, DATE_FORMAT);
                } catch (Exception e) {
                    ctx.status(400).json(new ApiError("INVALID_REQUEST", "Invalid date format"));
                    return;
                }

                if (!checkOut.isAfter(checkIn)) {
                    ctx.status(400).json(new ApiError("INVALID_REQUEST", "Check-out date must be after check-in date"));
                    return;
                }

                // Validate room exists
                Room room = RoomRepository.findById(request.roomNumber);
                if (room == null) {
                    ctx.status(404).json(new ApiError("ROOM_NOT_FOUND", "Room not found"));
                    return;
                }

                // Create reservation
                Reservation reservation = ReservationService.createReservation(
                    request.roomNumber,
                    request.guestName,
                    request.guestEmail,
                    request.guestCpf,
                    checkIn,
                    checkOut,
                    request.guests,
                    request.ratePlan,
                    request.companyCode
                );

                ctx.status(201).json(new ReservationResponse(reservation));
            } catch (RatePlan.InvalidRatePlanException e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", e.getMessage()));
            } catch (IllegalArgumentException e) {
                String message = e.getMessage();
                if (message.contains("Room not available")) {
                    ctx.status(409).json(new ApiError("ROOM_UNAVAILABLE", message));
                } else if (message.contains("capacity")) {
                    ctx.status(422).json(new ApiError("CAPACITY_EXCEEDED", message));
                } else if (message.contains("Rate plan")) {
                    ctx.status(400).json(new ApiError("INVALID_REQUEST", message));
                } else if (message.contains("Room not found")) {
                    ctx.status(404).json(new ApiError("ROOM_NOT_FOUND", message));
                } else {
                    ctx.status(400).json(new ApiError("INVALID_REQUEST", message));
                }
            } catch (SQLException e) {
                ctx.status(500).json(new ApiError("DATABASE_ERROR", e.getMessage()));
            }
        });

        // GET /reservations/{id}
        app.get("/reservations/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);
                if (reservation == null) {
                    ctx.status(404).json(new ApiError("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }
                ctx.json(new ReservationResponse(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (SQLException e) {
                ctx.status(500).json(new ApiError("DATABASE_ERROR", e.getMessage()));
            }
        });

        // POST /reservations/{id}/confirmation
        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = ReservationService.confirmReservation(id);
                ctx.json(new ReservationResponse(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(new ApiError("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ApiError("OPERATION_NOT_ALLOWED", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(new ApiError("DATABASE_ERROR", e.getMessage()));
            }
        });

        // POST /reservations/{id}/cancellation
        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = ReservationService.cancelReservation(id);
                ctx.json(new ReservationResponse(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(new ApiError("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ApiError("OPERATION_NOT_ALLOWED", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(new ApiError("DATABASE_ERROR", e.getMessage()));
            }
        });

        // POST /reservations/{id}/check-in
        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = ReservationService.checkIn(id);
                ctx.json(new ReservationResponse(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(new ApiError("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ApiError("OPERATION_NOT_ALLOWED", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(new ApiError("DATABASE_ERROR", e.getMessage()));
            }
        });

        // POST /reservations/{id}/check-out
        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = ReservationService.checkOut(id);
                Room room = RoomRepository.findById(reservation.roomNumber);
                ctx.json(new ReceiptResponse(reservation, room.type));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(new ApiError("RESERVATION_NOT_FOUND", e.getMessage()));
            } catch (IllegalStateException e) {
                ctx.status(409).json(new ApiError("OPERATION_NOT_ALLOWED", e.getMessage()));
            } catch (SQLException e) {
                ctx.status(500).json(new ApiError("DATABASE_ERROR", e.getMessage()));
            }
        });

        // GET /reservations/{id}/document
        app.get("/reservations/{id}/document", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);
                if (reservation == null) {
                    ctx.status(404).json(new ApiError("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (reservation.amountPaid.compareTo(BigDecimal.ZERO) == 0) {
                    ctx.status(404).json(new ApiError("RESERVATION_NOT_FOUND", "No document available for unpaid reservations"));
                    return;
                }

                Room room = RoomRepository.findById(reservation.roomNumber);
                ctx.json(new ReceiptResponse(reservation, room.type));
            } catch (NumberFormatException e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", "Invalid reservation ID"));
            } catch (SQLException e) {
                ctx.status(500).json(new ApiError("DATABASE_ERROR", e.getMessage()));
            }
        });

        // GET /messages
        app.get("/messages", ctx -> {
            try {
                var messages = MessageRepository.findAll();
                var response = messages.stream()
                    .map(MessageResponse::new)
                    .collect(Collectors.toList());
                ctx.json(response);
            } catch (SQLException e) {
                ctx.status(500).json(new ApiError("DATABASE_ERROR", e.getMessage()));
            }
        });

        // GET /reports/occupancy
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateParam = ctx.queryParam("date");
                if (dateParam == null) {
                    ctx.status(400).json(new ApiError("INVALID_REQUEST", "Missing date parameter"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateParam, DATE_FORMAT);
                String report = ReportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(report);
            } catch (Exception e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", "Invalid date format"));
            }
        });

        // GET /reports/revenue
        app.get("/reports/revenue", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null) {
                    ctx.status(400).json(new ApiError("INVALID_REQUEST", "Missing month parameter"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = ReportService.generateRevenueReport(month);
                ctx.contentType("text/csv; charset=utf-8").result(report);
            } catch (Exception e) {
                ctx.status(400).json(new ApiError("INVALID_REQUEST", "Invalid month format"));
            }
        });

        app.start(port);
        System.out.println("Server started on port " + port);
    }
}
