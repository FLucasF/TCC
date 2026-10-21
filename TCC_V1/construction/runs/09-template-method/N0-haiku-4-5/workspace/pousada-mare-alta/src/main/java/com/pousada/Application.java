package com.pousada;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.pousada.dto.*;
import com.pousada.model.*;
import com.pousada.repository.*;
import com.pousada.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class Application {
    public static void main(String[] args) {
        // Repositories
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        // Services
        ReservationService reservationService = new ReservationService(reservationRepository, roomRepository);
        MessageService messageService = new MessageService(messageRepository);
        DocumentService documentService = new DocumentService(roomRepository);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        // Configure Jackson for LocalDate handling
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Create Javalin app
        String port = System.getenv("PORT");
        int portNumber = port != null ? Integer.parseInt(port) : 7070;

        Javalin app = Javalin.create()
            .start(portNumber);

        // GET /rooms
        app.get("/rooms", ctx -> {
            var rooms = roomRepository.findAll();
            var roomDTOs = rooms.stream()
                .map(RoomDTO::from)
                .toList();
            ctx.json(roomDTOs);
        });

        // POST /reservations
        app.post("/reservations", ctx -> {
            try {
                var body = ctx.bodyAsClass(java.util.Map.class);

                // Validate
                if (!body.containsKey("roomNumber") || !body.containsKey("guestName") ||
                    !body.containsKey("guestEmail") || !body.containsKey("guestCpf") ||
                    !body.containsKey("checkIn") || !body.containsKey("checkOut") ||
                    !body.containsKey("guests") || !body.containsKey("ratePlan")) {
                    ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", "Missing required fields"));
                    return;
                }

                int roomNumber = ((Number) body.get("roomNumber")).intValue();
                String guestName = (String) body.get("guestName");
                String guestEmail = (String) body.get("guestEmail");
                String guestCpf = (String) body.get("guestCpf");
                String checkInStr = (String) body.get("checkIn");
                String checkOutStr = (String) body.get("checkOut");
                int guests = ((Number) body.get("guests")).intValue();
                String ratePlanStr = (String) body.get("ratePlan");

                LocalDate checkIn = LocalDate.parse(checkInStr, DateTimeFormatter.ISO_LOCAL_DATE);
                LocalDate checkOut = LocalDate.parse(checkOutStr, DateTimeFormatter.ISO_LOCAL_DATE);
                RatePlan ratePlan = RatePlan.valueOf(ratePlanStr);

                Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan
                );

                ctx.status(201).json(ReservationDTO.from(reservation));
            } catch (RuntimeException e) {
                handleError(ctx, e);
            }
        });

        // GET /reservations/{id}
        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                ctx.json(ReservationDTO.from(reservation));
            } catch (RuntimeException e) {
                handleError(ctx, e);
            }
        });

        // POST /reservations/{id}/confirmation
        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.confirmReservation(id);

                // Send confirmation email
                String comprovante = documentService.generateComprovante(reservation);
                messageService.sendConfirmationEmail(reservation, comprovante);

                ctx.json(ReservationDTO.from(reservation));
            } catch (RuntimeException e) {
                handleError(ctx, e);
            }
        });

        // POST /reservations/{id}/cancellation
        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(ReservationDTO.from(reservation));
            } catch (RuntimeException e) {
                handleError(ctx, e);
            }
        });

        // POST /reservations/{id}/check-in
        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(ReservationDTO.from(reservation));
            } catch (RuntimeException e) {
                handleError(ctx, e);
            }
        });

        // POST /reservations/{id}/check-out
        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkOut(id);

                // Generate comprovante
                String comprovante = documentService.generateComprovante(reservation);

                // Return comprovante as JSON
                ComprovanteDTO dto = new ComprovanteDTO();
                dto.reservationId = reservation.getId();
                dto.guestName = reservation.getGuestName();
                dto.guestCpf = reservation.getGuestCpf();
                dto.roomNumber = reservation.getRoomNumber();
                var room = roomRepository.findByNumber(reservation.getRoomNumber());
                if (room.isPresent()) {
                    dto.roomType = room.get().getType().toString();
                }
                dto.checkIn = reservation.getCheckIn();
                dto.checkOut = reservation.getCheckOut();
                dto.nights = reservation.getNights();
                dto.subtotal = reservation.getSubtotal();
                dto.discount = reservation.getDiscount();
                dto.total = reservation.getTotal();

                ctx.json(dto);
            } catch (RuntimeException e) {
                handleError(ctx, e);
            }
        });

        // GET /reservations/{id}/document
        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation.getStatus() == ReservationStatus.PENDING) {
                    ctx.status(409).json(new ErrorDTO("OPERATION_NOT_ALLOWED", "Reservation not paid yet"));
                    return;
                }

                // Return comprovante as JSON
                ComprovanteDTO dto = new ComprovanteDTO();
                dto.reservationId = reservation.getId();
                dto.guestName = reservation.getGuestName();
                dto.guestCpf = reservation.getGuestCpf();
                dto.roomNumber = reservation.getRoomNumber();
                var room = roomRepository.findByNumber(reservation.getRoomNumber());
                if (room.isPresent()) {
                    dto.roomType = room.get().getType().toString();
                }
                dto.checkIn = reservation.getCheckIn();
                dto.checkOut = reservation.getCheckOut();
                dto.nights = reservation.getNights();
                dto.subtotal = reservation.getSubtotal();
                dto.discount = reservation.getDiscount();
                dto.total = reservation.getTotal();

                ctx.json(dto);
            } catch (RuntimeException e) {
                handleError(ctx, e);
            }
        });

        // GET /messages
        app.get("/messages", ctx -> {
            var messages = messageService.getAllMessages();
            var messageDTOs = messages.stream()
                .map(MessageDTO::from)
                .toList();
            ctx.json(messageDTOs);
        });

        // GET /reports/occupancy
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isEmpty()) {
                    ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", "Missing date parameter"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                String report = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(report);
            } catch (Exception e) {
                ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", "Invalid date format"));
            }
        });

        // GET /reports/revenue
        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", "Missing month parameter"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = reportService.generateRevenueReport(month);
                ctx.contentType("text/csv; charset=utf-8").result(report);
            } catch (Exception e) {
                ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", "Invalid month format"));
            }
        });

        // GET /reports/tourism-tax
        app.get("/reports/tourism-tax", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", "Missing month parameter"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = reportService.generateTourismTaxReport(month);
                ctx.contentType("text/csv; charset=utf-8").result(report);
            } catch (Exception e) {
                ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", "Invalid month format"));
            }
        });

        // GET /reports/rate-plans
        app.get("/reports/rate-plans", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", "Missing month parameter"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = reportService.generateRatePlansReport(month);
                ctx.contentType("text/csv; charset=utf-8").result(report);
            } catch (Exception e) {
                ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", "Invalid month format"));
            }
        });
    }

    private static void handleError(io.javalin.http.Context ctx, RuntimeException e) {
        String message = e.getMessage();
        switch (message) {
            case "ROOM_NOT_FOUND" -> ctx.status(404).json(new ErrorDTO("ROOM_NOT_FOUND", "Room not found"));
            case "RESERVATION_NOT_FOUND" -> ctx.status(404).json(new ErrorDTO("RESERVATION_NOT_FOUND", "Reservation not found"));
            case "ROOM_UNAVAILABLE" -> ctx.status(409).json(new ErrorDTO("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
            case "OPERATION_NOT_ALLOWED" -> ctx.status(409).json(new ErrorDTO("OPERATION_NOT_ALLOWED", "Operation not allowed for this reservation status"));
            case "CAPACITY_EXCEEDED" -> ctx.status(422).json(new ErrorDTO("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
            case "INVALID_REQUEST" -> ctx.status(400).json(new ErrorDTO("INVALID_REQUEST", "Invalid request"));
            default -> ctx.status(500).json(new ErrorDTO("INTERNAL_ERROR", "Internal server error"));
        }
    }
}
