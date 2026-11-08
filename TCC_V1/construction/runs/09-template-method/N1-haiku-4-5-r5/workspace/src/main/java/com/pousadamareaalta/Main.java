package com.pousadamareaalta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamareaalta.database.*;
import com.pousadamareaalta.domain.*;
import com.pousadamareaalta.service.*;
import com.pousadamareaalta.service.report.*;
import com.pousadamareaalta.util.FormatterUtil;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws SQLException {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Database.initialize(dbUrl, dbUser, dbPassword);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JsonMapper() {
                @Override
                public String toJsonString(Object obj, Type type) {
                    try {
                        return mapper.writeValueAsString(obj);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public Object fromJsonString(String json, Type targetType) {
                    try {
                        return mapper.readValue(json, mapper.constructType(targetType));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        });

        app.get("/rooms", ctx -> {
            try {
                List<Room> rooms = RoomRepository.findAll();
                ctx.json(rooms);
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(Map.of("error", "DATABASE_ERROR", "message", e.getMessage()));
            }
        });

        app.post("/reservations", ctx -> {
            try {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);

                int roomNumber = ((Number) body.get("roomNumber")).intValue();
                String guestName = (String) body.get("guestName");
                String guestEmail = (String) body.get("guestEmail");
                String guestCpf = (String) body.get("guestCpf");
                LocalDate checkIn = LocalDate.parse((String) body.get("checkIn"));
                LocalDate checkOut = LocalDate.parse((String) body.get("checkOut"));
                int guests = ((Number) body.get("guests")).intValue();
                String ratePlan = (String) body.get("ratePlan");

                if (checkIn == null || checkOut == null) {
                    ctx.status(400);
                    ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
                    return;
                }

                Reservation created = ReservationService.create(
                    roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan
                );

                ctx.status(201);
                ctx.json(created);
            } catch (IllegalArgumentException e) {
                String errorCode = e.getMessage();
                if ("ROOM_NOT_FOUND".equals(errorCode)) {
                    ctx.status(404);
                } else if ("CAPACITY_EXCEEDED".equals(errorCode)) {
                    ctx.status(422);
                } else if ("ROOM_UNAVAILABLE".equals(errorCode)) {
                    ctx.status(409);
                } else {
                    ctx.status(400);
                }
                ctx.json(Map.of("error", errorCode, "message", errorCode));
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.getById(id);
                ctx.json(reservation);
            } catch (IllegalArgumentException e) {
                ctx.status(404);
                ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(Map.of("error", "DATABASE_ERROR", "message", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.confirm(id);

                Optional<Room> room = RoomRepository.findByNumber(reservation.roomNumber());
                if (room.isPresent()) {
                    MessageService.sendConfirmationEmail(reservation, room.get());
                }

                ctx.json(reservation);
            } catch (IllegalArgumentException e) {
                String errorCode = e.getMessage();
                if ("RESERVATION_NOT_FOUND".equals(errorCode)) {
                    ctx.status(404);
                } else {
                    ctx.status(409);
                }
                ctx.json(Map.of("error", errorCode, "message", errorCode));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(Map.of("error", "DATABASE_ERROR", "message", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.cancel(id);
                ctx.json(reservation);
            } catch (IllegalArgumentException e) {
                String errorCode = e.getMessage();
                if ("RESERVATION_NOT_FOUND".equals(errorCode)) {
                    ctx.status(404);
                } else {
                    ctx.status(409);
                }
                ctx.json(Map.of("error", errorCode, "message", errorCode));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(Map.of("error", "DATABASE_ERROR", "message", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.checkIn(id);
                ctx.json(reservation);
            } catch (IllegalArgumentException e) {
                String errorCode = e.getMessage();
                if ("RESERVATION_NOT_FOUND".equals(errorCode)) {
                    ctx.status(404);
                } else {
                    ctx.status(409);
                }
                ctx.json(Map.of("error", errorCode, "message", errorCode));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(Map.of("error", "DATABASE_ERROR", "message", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.checkOut(id);

                Optional<Room> room = RoomRepository.findByNumber(reservation.roomNumber());
                if (room.isEmpty()) {
                    ctx.status(500);
                    ctx.json(Map.of("error", "ROOM_NOT_FOUND", "message", "Room not found"));
                    return;
                }

                Receipt receipt = new Receipt(
                    "Comprovante de reserva",
                    reservation.id(),
                    reservation.guestName(),
                    reservation.guestCpf(),
                    reservation.roomNumber(),
                    room.get().type(),
                    reservation.checkIn(),
                    reservation.checkOut(),
                    reservation.nights(),
                    reservation.subtotal(),
                    reservation.discount(),
                    reservation.total()
                );

                ctx.json(receipt);
            } catch (IllegalArgumentException e) {
                String errorCode = e.getMessage();
                if ("RESERVATION_NOT_FOUND".equals(errorCode)) {
                    ctx.status(404);
                } else {
                    ctx.status(409);
                }
                ctx.json(Map.of("error", errorCode, "message", errorCode));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(Map.of("error", "DATABASE_ERROR", "message", e.getMessage()));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.getById(id);

                if (!"CONFIRMED".equals(reservation.status()) &&
                    !"CHECKED_IN".equals(reservation.status()) &&
                    !"CHECKED_OUT".equals(reservation.status())) {
                    ctx.status(409);
                    ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Second copy only available for paid reservations"));
                    return;
                }

                Optional<Room> room = RoomRepository.findByNumber(reservation.roomNumber());
                if (room.isEmpty()) {
                    ctx.status(500);
                    ctx.json(Map.of("error", "ROOM_NOT_FOUND", "message", "Room not found"));
                    return;
                }

                Receipt receipt = new Receipt(
                    "Comprovante de reserva",
                    reservation.id(),
                    reservation.guestName(),
                    reservation.guestCpf(),
                    reservation.roomNumber(),
                    room.get().type(),
                    reservation.checkIn(),
                    reservation.checkOut(),
                    reservation.nights(),
                    reservation.subtotal(),
                    reservation.discount(),
                    reservation.total()
                );

                ctx.json(receipt);
            } catch (IllegalArgumentException e) {
                ctx.status(404);
                ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(Map.of("error", "DATABASE_ERROR", "message", e.getMessage()));
            }
        });

        app.get("/messages", ctx -> {
            try {
                List<Message> messages = MessageService.getAllMessages();
                ctx.json(messages);
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(Map.of("error", "DATABASE_ERROR", "message", e.getMessage()));
            }
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                LocalDate date = LocalDate.parse(dateStr);

                ReportGenerator report = new OccupancyReport(date);
                String csv = report.generate();

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                YearMonth month = YearMonth.parse(monthStr);

                ReportGenerator report = new RevenueReport(month);
                String csv = report.generate();

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
            }
        });

        app.get("/reports/tourism-tax", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                YearMonth month = YearMonth.parse(monthStr);

                ReportGenerator report = new TourismTaxReport(month);
                String csv = report.generate();

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
            }
        });

        app.get("/reports/rate-plans", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                YearMonth month = YearMonth.parse(monthStr);

                ReportGenerator report = new RatePlansReport(month);
                String csv = report.generate();

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
            }
        });

        app.start(port);
    }
}
