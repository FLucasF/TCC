package com.pousamarealta.controllers;

import io.javalin.Javalin;
import com.pousamarealta.dtos.CreateReservationRequest;
import com.pousamarealta.dtos.ErrorResponse;
import com.pousamarealta.models.*;
import com.pousamarealta.services.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReservationController {
    private ReservationService reservationService;
    private MessageService messageService;
    private ReportService reportService;
    private RoomService roomService;

    public ReservationController(ReservationService reservationService, MessageService messageService, ReportService reportService) {
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.reportService = reportService;
        this.roomService = new RoomService();
    }

    public void setupRoutes(Javalin app) {
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
    }

    private void createReservation(io.javalin.http.Context ctx) {
        try {
            CreateReservationRequest req;
            try {
                req = ctx.bodyAsClass(CreateReservationRequest.class);
            } catch (Exception e) {
                System.err.println("Error parsing request: " + e.getMessage());
                e.printStackTrace();
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid JSON body"));
                return;
            }

            if (req == null || req.roomNumber == 0 || req.guestName == null || req.guestEmail == null ||
                req.checkIn == null || req.checkOut == null || req.ratePlan == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            int identificationCount = 0;
            if (req.guestCpf != null) identificationCount++;
            if (req.companyCnpj != null) identificationCount++;
            if (req.guestPassport != null) identificationCount++;

            if (identificationCount != 1) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Exactly one identification method required"));
                return;
            }

            if (req.companyCnpj != null && req.companyName == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Company name required for CNPJ"));
                return;
            }

            if (req.guestPassport != null && req.guestCountry == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Country required for passport"));
                return;
            }

            Room room = roomService.getRoomByNumber(req.roomNumber);
            if (room == null) {
                ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                return;
            }

            if (req.guests < 1 || req.guests > room.getCapacity()) {
                ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Guest count exceeds room capacity"));
                return;
            }

            LocalDate checkIn, checkOut;
            try {
                checkIn = LocalDate.parse(req.checkIn, DateTimeFormatter.ISO_DATE);
                checkOut = LocalDate.parse(req.checkOut, DateTimeFormatter.ISO_DATE);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Check-out must be after check-in"));
                return;
            }

            if (!reservationService.isRoomAvailable(req.roomNumber, checkIn, checkOut)) {
                ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room unavailable for selected dates"));
                return;
            }

            RatePlan ratePlan;
            try {
                ratePlan = RatePlan.fromString(req.ratePlan);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
                return;
            }

            Reservation reservation = reservationService.createReservation(
                req.roomNumber, req.guestName, req.guestEmail,
                req.guestCpf, req.companyCnpj, req.companyName,
                req.guestPassport, req.guestCountry,
                checkIn, checkOut, req.guests, ratePlan
            );

            ctx.status(201).json(reservation);
        } catch (Exception e) {
            System.err.println("Unexpected error in createReservation: " + e.getMessage());
            e.printStackTrace();
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    private void getReservation(io.javalin.http.Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(reservation);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private void confirmReservation(io.javalin.http.Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.PENDING) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Reservation cannot be confirmed"));
                return;
            }

            Reservation confirmed = reservationService.confirmReservation(id);

            Document doc = DocumentService.generateDocument(confirmed, roomService.getRoomByNumber(confirmed.getRoomNumber()));
            String emailBody = generateConfirmationEmail(confirmed.getGuestName(), doc.getText());
            messageService.sendMessage("EMAIL", confirmed.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta", emailBody);

            ctx.json(confirmed);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private void cancelReservation(io.javalin.http.Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.PENDING &&
                reservation.getStatus() != ReservationStatus.CONFIRMED) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Reservation cannot be cancelled"));
                return;
            }

            Reservation cancelled = reservationService.cancelReservation(id);
            ctx.json(cancelled);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private void checkIn(io.javalin.http.Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Check-in not allowed"));
                return;
            }

            Reservation checkedIn = reservationService.checkIn(id);
            ctx.json(checkedIn);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private void checkOut(io.javalin.http.Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Check-out not allowed"));
                return;
            }

            Reservation checkedOut = reservationService.checkOut(id);
            Room room = roomService.getRoomByNumber(checkedOut.getRoomNumber());
            Document doc = DocumentService.generateDocument(checkedOut, room);

            ctx.json(doc);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private void getDocument(io.javalin.http.Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.getReservation(id);

            if (reservation == null) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
                reservation.getStatus() != ReservationStatus.CHECKED_IN &&
                reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Document not available"));
                return;
            }

            Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
            Document doc = DocumentService.generateDocument(reservation, room);

            ctx.json(doc);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
        }
    }

    private void getMessages(io.javalin.http.Context ctx) {
        ctx.json(messageService.getAllMessages());
    }

    private void getOccupancyReport(io.javalin.http.Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null || dateParam.isEmpty()) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Date parameter required"));
                return;
            }

            LocalDate date = LocalDate.parse(dateParam, DateTimeFormatter.ISO_DATE);
            String report = reportService.getOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    private void getRevenueReport(io.javalin.http.Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isEmpty()) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Month parameter required"));
                return;
            }

            YearMonth month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
            String report = reportService.getRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    private String generateConfirmationEmail(String guestName, String documentText) {
        return "Olá, " + guestName + "!\n\n" +
               "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
               documentText + "\n\n" +
               "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
               "Até breve!";
    }
}
