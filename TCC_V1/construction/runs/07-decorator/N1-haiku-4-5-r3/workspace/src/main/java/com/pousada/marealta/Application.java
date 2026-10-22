package com.pousada.marealta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pousada.marealta.api.ErrorResponse;
import com.pousada.marealta.db.Database;
import com.pousada.marealta.db.RoomRepository;
import com.pousada.marealta.db.ReservationRepository;
import com.pousada.marealta.db.MessageRepository;
import com.pousada.marealta.model.*;
import com.pousada.marealta.service.DocumentService;
import com.pousada.marealta.service.MessageService;
import com.pousada.marealta.service.ReportService;
import com.pousada.marealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Application {
    private static final Gson gson = new GsonBuilder()
            .create();
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) throws Exception {
        Database.initialize();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        Javalin app = Javalin.create().start(port);

        app.get("/rooms", ctx -> {
            try {
                List<Room> rooms = RoomRepository.findAll();
                ctx.json(rooms.stream().map(r -> Map.of(
                        "number", r.number,
                        "type", r.type.name(),
                        "capacity", r.capacity,
                        "nightlyRate", r.nightlyRate.doubleValue()
                )).toList());
            } catch (Exception e) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations", ctx -> {
            try {
                Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);
                validateRequiredFields(body, "roomNumber", "guestName", "guestEmail", "guestCpf", "checkIn", "checkOut", "guests", "ratePlan");

                int roomNumber = ((Double) body.get("roomNumber")).intValue();
                String guestName = (String) body.get("guestName");
                String guestEmail = (String) body.get("guestEmail");
                String guestCpf = (String) body.get("guestCpf");
                LocalDate checkIn = LocalDate.parse((String) body.get("checkIn"), dateFormatter);
                LocalDate checkOut = LocalDate.parse((String) body.get("checkOut"), dateFormatter);
                int guests = ((Double) body.get("guests")).intValue();
                RatePlan ratePlan = RatePlan.valueOf((String) body.get("ratePlan"));

                if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
                    ctx.status(HttpStatus.BAD_REQUEST);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Check-out must be after check-in"));
                    return;
                }

                Reservation reservation = ReservationService.createReservation(roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan);
                ctx.status(HttpStatus.CREATED);
                ctx.json(reservationToJson(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request format"));
            }
        });

        app.get("/reservations/:id", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);
                if (reservation == null) {
                    ctx.status(HttpStatus.NOT_FOUND);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }
                ctx.json(reservationToJson(reservation));
            } catch (NumberFormatException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/:id/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);
                BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());

                Reservation reservation = ReservationService.confirmReservation(id, amountPaid);
                ctx.json(reservationToJson(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request format"));
            }
        });

        app.post("/reservations/:id/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.cancelReservation(id);
                ctx.json(reservationToJson(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/:id/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.checkIn(id);
                ctx.json(reservationToJson(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.post("/reservations/:id/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.checkOut(id);
                Room room = RoomRepository.findByNumber(reservation.roomNumber);
                Document document = new Document(reservation.id, reservation.guestName, reservation.guestCpf,
                        reservation.roomNumber, room.type.name(), reservation.checkIn, reservation.checkOut,
                        reservation.nights, reservation.subtotal, reservation.discount, reservation.total);
                ctx.json(documentToJson(document));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.get("/reservations/:id/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationRepository.findById(id);
                if (reservation == null) {
                    ctx.status(HttpStatus.NOT_FOUND);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }

                if (reservation.amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
                    ctx.status(HttpStatus.NOT_FOUND);
                    ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Document not available for unpaid reservations"));
                    return;
                }

                Room room = RoomRepository.findByNumber(reservation.roomNumber);
                Document document = new Document(reservation.id, reservation.guestName, reservation.guestCpf,
                        reservation.roomNumber, room.type.name(), reservation.checkIn, reservation.checkOut,
                        reservation.nights, reservation.subtotal, reservation.discount, reservation.total);
                ctx.json(documentToJson(document));
            } catch (NumberFormatException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.get("/messages", ctx -> {
            try {
                List<Message> messages = MessageService.getAllMessages();
                ctx.json(messages.stream().map(m -> Map.of(
                        "id", m.id,
                        "channel", m.channel,
                        "to", m.to,
                        "subject", m.subject,
                        "body", m.body,
                        "status", m.status.name()
                )).toList());
            } catch (Exception e) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null) {
                    ctx.status(HttpStatus.BAD_REQUEST);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Date parameter required"));
                    return;
                }
                LocalDate date = LocalDate.parse(dateStr, dateFormatter);
                String report = ReportService.generateOccupancyReport(date);
                ctx.contentType("text/csv;charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    ctx.status(HttpStatus.BAD_REQUEST);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Month parameter required"));
                    return;
                }
                String[] parts = monthStr.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                String report = ReportService.generateRevenueReport(year, month);
                ctx.contentType("text/csv;charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
            }
        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        });
    }

    private static void validateRequiredFields(Map<String, Object> body, String... fields) throws Exception {
        for (String field : fields) {
            if (body.get(field) == null) {
                throw new IllegalArgumentException("INVALID_REQUEST");
            }
        }
    }

    private static void handleError(Context ctx, String errorCode) {
        switch (errorCode) {
            case "ROOM_NOT_FOUND":
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                break;
            case "RESERVATION_NOT_FOUND":
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                break;
            case "ROOM_UNAVAILABLE":
                ctx.status(HttpStatus.CONFLICT);
                ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for these dates"));
                break;
            case "CAPACITY_EXCEEDED":
                ctx.status(422);
                ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
                break;
            case "OPERATION_NOT_ALLOWED":
                ctx.status(HttpStatus.CONFLICT);
                ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current reservation status"));
                break;
            default:
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    private static Map<String, Object> reservationToJson(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.id);
        map.put("roomNumber", r.roomNumber);
        map.put("guestName", r.guestName);
        map.put("guestEmail", r.guestEmail);
        map.put("guestCpf", r.guestCpf);
        map.put("checkIn", r.checkIn.toString());
        map.put("checkOut", r.checkOut.toString());
        map.put("guests", r.guests);
        map.put("ratePlan", r.ratePlan.name());
        map.put("nights", r.nights);
        map.put("subtotal", r.subtotal.doubleValue());
        map.put("discount", r.discount.doubleValue());
        map.put("total", r.total.doubleValue());
        map.put("status", r.status.name());
        map.put("amountPaid", r.amountPaid.doubleValue());
        map.put("refund", r.refund != null ? r.refund.doubleValue() : null);
        return map;
    }

    private static Map<String, Object> documentToJson(Document d) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", d.title);
        map.put("reservationId", d.reservationId);
        map.put("guestName", d.guestName);
        map.put("guestCpf", d.guestCpf);
        map.put("roomNumber", d.roomNumber);
        map.put("roomType", d.roomType);
        map.put("checkIn", d.checkIn.toString());
        map.put("checkOut", d.checkOut.toString());
        map.put("nights", d.nights);
        map.put("subtotal", d.subtotal.doubleValue());
        map.put("discount", d.discount.doubleValue());
        map.put("total", d.total.doubleValue());
        return map;
    }
}
