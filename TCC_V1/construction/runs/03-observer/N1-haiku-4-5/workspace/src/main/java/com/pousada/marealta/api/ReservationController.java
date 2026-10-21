package com.pousada.marealta.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.javalin.Javalin;
import io.javalin.http.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.pousada.marealta.model.RatePlan;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.ReservationStatus;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.service.MessageService;
import com.pousada.marealta.service.OccupancyReportService;
import com.pousada.marealta.service.ReservationService;
import com.pousada.marealta.service.RevenueReportService;
import com.pousada.marealta.service.RoomService;

public class ReservationController {
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final OccupancyReportService occupancyReportService;
    private final RevenueReportService revenueReportService;

    public ReservationController(RoomService roomService, ReservationService reservationService,
                                 MessageService messageService, OccupancyReportService occupancyReportService,
                                 RevenueReportService revenueReportService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.occupancyReportService = occupancyReportService;
        this.revenueReportService = revenueReportService;
    }

    public void registerRoutes(Javalin app) {
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
    }

    private void getRooms(Context ctx) {
        List<RoomResponse> rooms = roomService.getAllRooms().stream()
            .map(RoomResponse::new)
            .toList();
        ctx.json(rooms);
    }

    private void createReservation(Context ctx) {
        try {
            JsonNode body = ctx.bodyAsClass(JsonNode.class);

            int roomNumber = body.get("roomNumber").asInt();
            String guestName = body.get("guestName").asText();
            String guestEmail = body.get("guestEmail").asText();
            String guestCpf = body.get("guestCpf").asText();
            String checkInStr = body.get("checkIn").asText();
            String checkOutStr = body.get("checkOut").asText();
            int guests = body.get("guests").asInt();
            String ratePlanStr = body.get("ratePlan").asText();

            LocalDate checkIn = LocalDate.parse(checkInStr);
            LocalDate checkOut = LocalDate.parse(checkOutStr);
            RatePlan ratePlan = RatePlan.valueOf(ratePlanStr);

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(new ReservationResponse(reservation));
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        } catch (IllegalArgumentException e) {
            handleServiceException(ctx, e);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> reservation = reservationService.getReservation(id);

            if (reservation.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(new ReservationResponse(reservation.get()));
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            JsonNode body = ctx.bodyAsClass(JsonNode.class);
            BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").asText());

            Reservation reservation = reservationService.confirmReservation(id, amountPaid);
            ctx.json(new ReservationResponse(reservation));
        } catch (IllegalArgumentException e) {
            handleServiceException(ctx, e);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(new ReservationResponse(reservation));
        } catch (IllegalArgumentException e) {
            handleServiceException(ctx, e);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(new ReservationResponse(reservation));
        } catch (IllegalArgumentException e) {
            handleServiceException(ctx, e);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            Optional<Room> room = roomService.getRoomByNumber(reservation.roomNumber);
            ctx.json(new ReceiptResponse(reservation, room.get()));
        } catch (IllegalArgumentException e) {
            handleServiceException(ctx, e);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> reservation = reservationService.getReservation(id);

            if (reservation.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            Reservation res = reservation.get();
            if (res.status != ReservationStatus.CONFIRMED && res.status != ReservationStatus.CHECKED_IN &&
                res.status != ReservationStatus.CHECKED_OUT) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "No document available"));
                return;
            }

            Optional<Room> room = roomService.getRoomByNumber(res.roomNumber);
            ctx.json(new ReceiptResponse(res, room.get()));
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    private void getMessages(Context ctx) {
        List<MessageResponse> messages = messageService.getAllMessages().stream()
            .map(MessageResponse::new)
            .toList();
        ctx.json(messages);
    }

    private void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String report = occupancyReportService.generateOccupancyReport(date);
            ctx.contentType("text/csv;charset=utf-8").result(report);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    private void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            String report = revenueReportService.generateRevenueReport(monthStr);
            ctx.contentType("text/csv;charset=utf-8").result(report);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    private void handleServiceException(Context ctx, IllegalArgumentException e) {
        String message = e.getMessage();
        switch (message) {
            case "ROOM_NOT_FOUND":
                ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                break;
            case "RESERVATION_NOT_FOUND":
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                break;
            case "ROOM_UNAVAILABLE":
                ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is unavailable for the selected dates"));
                break;
            case "CAPACITY_EXCEEDED":
                ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
                break;
            case "OPERATION_NOT_ALLOWED":
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed for this reservation state"));
                break;
            default:
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", message));
        }
    }
}
