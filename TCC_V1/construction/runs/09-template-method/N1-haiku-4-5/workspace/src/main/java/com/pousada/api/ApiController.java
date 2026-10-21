package com.pousada.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pousada.domain.*;
import com.pousada.service.*;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiController {
    private final ReservationService reservationService;
    private final ReportService reportService;
    private final RoomRepository roomRepo;
    private final MessageRepository messageRepo;
    private final ReservationRepository reservationRepo;
    private final Gson gson;

    public ApiController(ReservationService reservationService, ReportService reportService,
                        RoomRepository roomRepo, MessageRepository messageRepo, ReservationRepository reservationRepo) {
        this.reservationService = reservationService;
        this.reportService = reportService;
        this.roomRepo = roomRepo;
        this.messageRepo = messageRepo;
        this.reservationRepo = reservationRepo;
        this.gson = new Gson();
    }

    public void setupRoutes(Javalin app) {
        app.get("/rooms", ctx -> {
            var rooms = roomRepo.getAllRooms();
            var roomList = rooms.stream().map(room -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("number", room.getNumber());
                data.put("type", room.getType().getDisplayName());
                data.put("capacity", room.getCapacity());
                data.put("nightlyRate", room.getNightlyRate().doubleValue());
                return data;
            }).toList();
            ctx.json(roomList);
        });

        app.post("/reservations", ctx -> {
            try {
                var body = gson.fromJson(ctx.body(), JsonObject.class);

                if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
                    !body.has("guestCpf") || !body.has("checkIn") || !body.has("checkOut") ||
                    !body.has("guests") || !body.has("ratePlan")) {
                    ctx.status(400).json(error("INVALID_REQUEST", "Missing required fields"));
                    return;
                }

                int roomNumber = body.get("roomNumber").getAsInt();
                String guestName = body.get("guestName").getAsString();
                String guestEmail = body.get("guestEmail").getAsString();
                String guestCpf = body.get("guestCpf").getAsString();
                String checkInStr = body.get("checkIn").getAsString();
                String checkOutStr = body.get("checkOut").getAsString();
                int guests = body.get("guests").getAsInt();
                String ratePlanStr = body.get("ratePlan").getAsString();

                LocalDate checkIn;
                LocalDate checkOut;
                try {
                    checkIn = LocalDate.parse(checkInStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    checkOut = LocalDate.parse(checkOutStr, DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (DateTimeParseException e) {
                    ctx.status(400).json(error("INVALID_REQUEST", "Invalid date format"));
                    return;
                }

                if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
                    ctx.status(400).json(error("INVALID_REQUEST", "Checkout must be after checkin"));
                    return;
                }

                if (guests < 1) {
                    ctx.status(400).json(error("INVALID_REQUEST", "At least one guest required"));
                    return;
                }

                Room room = roomRepo.getRoom(roomNumber);
                if (room == null) {
                    ctx.status(404).json(error("ROOM_NOT_FOUND", "Room not found"));
                    return;
                }

                if (guests > room.getCapacity()) {
                    ctx.status(422).json(error("CAPACITY_EXCEEDED", "Too many guests for this room"));
                    return;
                }

                RatePlan ratePlan;
                try {
                    ratePlan = RatePlan.valueOf(ratePlanStr);
                } catch (IllegalArgumentException e) {
                    ctx.status(400).json(error("INVALID_REQUEST", "Invalid rate plan"));
                    return;
                }

                if (!isRoomAvailable(roomNumber, checkIn, checkOut)) {
                    ctx.status(409).json(error("ROOM_UNAVAILABLE", "Room not available for these dates"));
                    return;
                }

                try {
                    Reservation reservation = reservationService.createReservation(roomNumber, guestName, guestEmail,
                            guestCpf, checkIn, checkOut, guests, ratePlan);

                    ctx.status(201).json(reservationToJson(reservation));
                } catch (IllegalArgumentException e) {
                    ctx.status(400).json(error("INVALID_REQUEST", e.getMessage()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(400).json(error("INVALID_REQUEST", "Invalid request body"));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation == null) {
                    ctx.status(404).json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                ctx.json(reservationToJson(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400).json(error("INVALID_REQUEST", "Invalid reservation id"));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation == null) {
                    ctx.status(404).json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (reservation.getStatus() != ReservationStatus.PENDING) {
                    ctx.status(409).json(error("OPERATION_NOT_ALLOWED", "Reservation cannot be confirmed"));
                    return;
                }

                Reservation confirmed = reservationService.confirmReservation(id, reservation.getTotal());
                ctx.json(reservationToJson(confirmed));
            } catch (NumberFormatException e) {
                ctx.status(400).json(error("INVALID_REQUEST", "Invalid reservation id"));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation == null) {
                    ctx.status(404).json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                Reservation cancelled = reservationService.cancelReservation(id);
                if (cancelled == null) {
                    ctx.status(409).json(error("OPERATION_NOT_ALLOWED", "Reservation cannot be cancelled"));
                    return;
                }

                ctx.json(reservationToJson(cancelled));
            } catch (NumberFormatException e) {
                ctx.status(400).json(error("INVALID_REQUEST", "Invalid reservation id"));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation == null) {
                    ctx.status(404).json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
                    ctx.status(409).json(error("OPERATION_NOT_ALLOWED", "Reservation cannot be checked in"));
                    return;
                }

                Reservation checkedIn = reservationService.checkIn(id);
                ctx.json(reservationToJson(checkedIn));
            } catch (NumberFormatException e) {
                ctx.status(400).json(error("INVALID_REQUEST", "Invalid reservation id"));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation == null) {
                    ctx.status(404).json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
                    ctx.status(409).json(error("OPERATION_NOT_ALLOWED", "Reservation cannot be checked out"));
                    return;
                }

                Reservation checkedOut = reservationService.checkOut(id);
                Map<String, Object> receipt = receiptToJson(reservation);
                ctx.json(receipt);
            } catch (NumberFormatException e) {
                ctx.status(400).json(error("INVALID_REQUEST", "Invalid reservation id"));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation == null) {
                    ctx.status(404).json(error("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (reservation.getStatus() == ReservationStatus.PENDING || reservation.getStatus() == ReservationStatus.CANCELLED) {
                    ctx.status(404).json(error("RESERVATION_NOT_FOUND", "Document not available for this reservation"));
                    return;
                }

                Map<String, Object> receipt = receiptToJson(reservation);
                ctx.json(receipt);
            } catch (NumberFormatException e) {
                ctx.status(400).json(error("INVALID_REQUEST", "Invalid reservation id"));
            }
        });

        app.get("/messages", ctx -> {
            var messages = messageRepo.getAllMessages();
            var messageList = messages.stream().map(msg -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("id", msg.getId());
                data.put("channel", msg.getChannel());
                data.put("to", msg.getTo());
                data.put("subject", msg.getSubject());
                data.put("body", msg.getBody());
                return data;
            }).toList();
            ctx.json(messageList);
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isEmpty()) {
                    ctx.status(400).json(error("INVALID_REQUEST", "Date parameter is required"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                String report = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv;charset=utf-8").result(report);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(error("INVALID_REQUEST", "Invalid date format"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400).json(error("INVALID_REQUEST", "Month parameter is required"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthStr);
                String report = reportService.generateRevenueReport(month);
                ctx.contentType("text/csv;charset=utf-8").result(report);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(error("INVALID_REQUEST", "Invalid month format"));
            }
        });

        app.get("/reports/tourism-tax", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400).json(error("INVALID_REQUEST", "Month parameter is required"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthStr);
                String report = reportService.generateTourismTaxReport(month);
                ctx.contentType("text/csv;charset=utf-8").result(report);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(error("INVALID_REQUEST", "Invalid month format"));
            }
        });

        app.get("/reports/rate-plans", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400).json(error("INVALID_REQUEST", "Month parameter is required"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthStr);
                String report = reportService.generateRatePlansReport(month);
                ctx.contentType("text/csv;charset=utf-8").result(report);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(error("INVALID_REQUEST", "Invalid month format"));
            }
        });
    }

    private boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation r : reservationRepo.getAllReservations()) {
            if (r.getRoomNumber() != roomNumber) continue;
            if (r.getStatus() == ReservationStatus.CANCELLED) continue;

            LocalDate rCheckIn = r.getCheckIn();
            LocalDate rCheckOut = r.getCheckOut();

            if (!(checkOut.isEqual(rCheckIn) || checkOut.isBefore(rCheckIn)) &&
                !(checkIn.isEqual(rCheckOut) || checkIn.isAfter(rCheckOut))) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> reservationToJson(Reservation reservation) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", reservation.getId());
        data.put("roomNumber", reservation.getRoomNumber());
        data.put("guestName", reservation.getGuestName());
        data.put("guestEmail", reservation.getGuestEmail());
        data.put("guestCpf", reservation.getGuestCpf());
        data.put("checkIn", reservation.getCheckIn());
        data.put("checkOut", reservation.getCheckOut());
        data.put("guests", reservation.getGuests());
        data.put("ratePlan", reservation.getRatePlan().name());
        data.put("nights", reservation.getNights());
        data.put("subtotal", round2(reservation.getSubtotal().doubleValue()));
        data.put("discount", round2(reservation.getDiscount().doubleValue()));
        data.put("total", round2(reservation.getTotal().doubleValue()));
        data.put("status", reservation.getStatus().name());
        data.put("amountPaid", round2(reservation.getAmountPaid().doubleValue()));
        data.put("refund", reservation.getRefund() != null ? round2(reservation.getRefund().doubleValue()) : null);
        return data;
    }

    private Map<String, Object> receiptToJson(Reservation reservation) {
        Room room = roomRepo.getRoom(reservation.getRoomNumber());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", "Comprovante de reserva");
        data.put("reservationId", reservation.getId());
        data.put("guestName", reservation.getGuestName());
        data.put("guestCpf", reservation.getGuestCpf());
        data.put("roomNumber", room.getNumber());
        data.put("roomType", room.getType().getDisplayName());
        data.put("checkIn", reservation.getCheckIn());
        data.put("checkOut", reservation.getCheckOut());
        data.put("nights", reservation.getNights());
        data.put("subtotal", round2(reservation.getSubtotal().doubleValue()));
        data.put("discount", round2(reservation.getDiscount().doubleValue()));
        data.put("total", round2(reservation.getTotal().doubleValue()));
        return data;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Map<String, String> error(String code, String message) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("error", code);
        data.put("message", message);
        return data;
    }
}
