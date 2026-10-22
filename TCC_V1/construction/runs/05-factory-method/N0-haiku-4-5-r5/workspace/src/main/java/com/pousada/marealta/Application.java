package com.pousada.marealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.dto.DocumentResponse;
import com.pousada.marealta.dto.ReservationResponse;
import com.pousada.marealta.model.Message;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.repository.RoomRepository;
import com.pousada.marealta.service.DocumentService;
import com.pousada.marealta.service.ReportService;
import com.pousada.marealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws SQLException {
        DatabaseConfig.initialize();

        String portStr = System.getenv("PORT");
        int port = portStr != null && !portStr.isEmpty() ? Integer.parseInt(portStr) : 7070;

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new io.javalin.json.JsonMapper() {
                private final ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                @Override
                public String toJsonString(Object obj, java.lang.reflect.Type type) {
                    try {
                        return mapper.writeValueAsString(obj);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public Object fromJsonString(String json, java.lang.reflect.Type targetType) {
                    try {
                        return mapper.readValue(json, mapper.constructType(targetType));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        });

        // GET /rooms
        app.get("/rooms", ctx -> {
            try {
                List<Room> rooms = RoomRepository.findAll();
                List<Map<String, Object>> response = rooms.stream().map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("number", r.getNumber());
                    map.put("type", r.getType().name());
                    map.put("capacity", r.getCapacity());
                    map.put("nightlyRate", r.getNightlyRate());
                    return map;
                }).toList();
                ctx.json(response);
            } catch (SQLException e) {
                sendError(ctx, 500, "DATABASE_ERROR", e.getMessage());
            }
        });

        // POST /reservations
        app.post("/reservations", ctx -> {
            try {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);

                Integer roomNumber = getIntValue(body, "roomNumber");
                String guestName = getStringValue(body, "guestName");
                String guestEmail = getStringValue(body, "guestEmail");
                LocalDate checkIn = getDateValue(body, "checkIn");
                LocalDate checkOut = getDateValue(body, "checkOut");
                Integer guests = getIntValue(body, "guests");
                String ratePlan = getStringValue(body, "ratePlan");
                String guestCpf = getStringValue(body, "guestCpf");
                String companyCnpj = getStringValue(body, "companyCnpj");
                String companyName = getStringValue(body, "companyName");
                String guestPassport = getStringValue(body, "guestPassport");
                String guestCountry = getStringValue(body, "guestCountry");

                if (roomNumber == null || guestName == null || guestEmail == null ||
                    checkIn == null || checkOut == null || guests == null || ratePlan == null) {
                    sendError(ctx, 400, "INVALID_REQUEST", "Missing required fields");
                    return;
                }

                Reservation reservation = ReservationService.createReservation(
                    roomNumber, guestName, guestEmail, checkIn, checkOut, guests, ratePlan,
                    guestCpf, companyCnpj, companyName, guestPassport, guestCountry
                );

                ctx.status(201).json(ReservationResponse.from(reservation));
            } catch (IllegalArgumentException e) {
                handleServiceException(ctx, e);
            } catch (SQLException e) {
                sendError(ctx, 500, "DATABASE_ERROR", e.getMessage());
            } catch (Exception e) {
                sendError(ctx, 400, "INVALID_REQUEST", e.getMessage());
            }
        });

        // GET /reservations/{id}
        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.getReservation(id);
                if (reservation == null) {
                    sendError(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
                } else {
                    ctx.json(ReservationResponse.from(reservation));
                }
            } catch (SQLException e) {
                sendError(ctx, 500, "DATABASE_ERROR", e.getMessage());
            }
        });

        // POST /reservations/{id}/confirmation
        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Map<String, Object> body = ctx.bodyAsClass(Map.class);

                Object amountPaidObj = body.get("amountPaid");
                if (amountPaidObj == null) {
                    sendError(ctx, 400, "INVALID_REQUEST", "amountPaid is required");
                    return;
                }

                BigDecimal amountPaid = new BigDecimal(amountPaidObj.toString());
                Reservation reservation = ReservationService.confirmReservation(id, amountPaid);
                ctx.json(ReservationResponse.from(reservation));
            } catch (IllegalArgumentException e) {
                handleServiceException(ctx, e);
            } catch (SQLException e) {
                sendError(ctx, 500, "DATABASE_ERROR", e.getMessage());
            } catch (Exception e) {
                sendError(ctx, 400, "INVALID_REQUEST", e.getMessage());
            }
        });

        // POST /reservations/{id}/cancellation
        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.cancelReservation(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (IllegalArgumentException e) {
                handleServiceException(ctx, e);
            } catch (SQLException e) {
                sendError(ctx, 500, "DATABASE_ERROR", e.getMessage());
            }
        });

        // POST /reservations/{id}/check-in
        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.checkIn(id);
                ctx.json(ReservationResponse.from(reservation));
            } catch (IllegalArgumentException e) {
                handleServiceException(ctx, e);
            } catch (SQLException e) {
                sendError(ctx, 500, "DATABASE_ERROR", e.getMessage());
            }
        });

        // POST /reservations/{id}/check-out
        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.checkOut(id);
                DocumentResponse doc = DocumentService.generateDocument(reservation);
                ctx.json(doc);
            } catch (IllegalArgumentException e) {
                handleServiceException(ctx, e);
            } catch (SQLException e) {
                sendError(ctx, 500, "DATABASE_ERROR", e.getMessage());
            }
        });

        // GET /reservations/{id}/document
        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = ReservationService.getReservation(id);
                if (reservation == null) {
                    sendError(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
                } else if (reservation.getStatus() == Reservation.Status.PENDING) {
                    sendError(ctx, 409, "OPERATION_NOT_ALLOWED", "Document only available for paid reservations");
                } else {
                    DocumentResponse doc = DocumentService.generateDocument(reservation);
                    ctx.json(doc);
                }
            } catch (SQLException e) {
                sendError(ctx, 500, "DATABASE_ERROR", e.getMessage());
            }
        });

        // GET /messages
        app.get("/messages", ctx -> {
            try {
                List<Message> messages = MessageRepository.findAll();
                List<Map<String, Object>> response = messages.stream().map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.getId());
                    map.put("channel", m.getChannel().name());
                    map.put("to", m.getTo());
                    map.put("subject", m.getSubject());
                    map.put("body", m.getBody());
                    return map;
                }).toList();
                ctx.json(response);
            } catch (SQLException e) {
                sendError(ctx, 500, "DATABASE_ERROR", e.getMessage());
            }
        });

        // GET /reports/occupancy
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isEmpty()) {
                    sendError(ctx, 400, "INVALID_REQUEST", "date parameter is required");
                    return;
                }
                LocalDate date = LocalDate.parse(dateStr);
                String report = ReportService.getOccupancyReport(date);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                sendError(ctx, 400, "INVALID_REQUEST", e.getMessage());
            }
        });

        // GET /reports/revenue
        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    sendError(ctx, 400, "INVALID_REQUEST", "month parameter is required");
                    return;
                }
                YearMonth month = YearMonth.parse(monthStr);
                String report = ReportService.getRevenueReport(month);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                sendError(ctx, 400, "INVALID_REQUEST", e.getMessage());
            }
        });

        app.start(port);
        logger.info("Pousada Maré Alta API started on port {}", port);
    }

    private static void handleServiceException(Context ctx, IllegalArgumentException e) {
        String message = e.getMessage();
        if (message.equals("ROOM_NOT_FOUND")) {
            sendError(ctx, 404, "ROOM_NOT_FOUND", "Room not found");
        } else if (message.equals("RESERVATION_NOT_FOUND")) {
            sendError(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
        } else if (message.equals("ROOM_UNAVAILABLE")) {
            sendError(ctx, 409, "ROOM_UNAVAILABLE", "Room is not available for the selected dates");
        } else if (message.equals("OPERATION_NOT_ALLOWED")) {
            sendError(ctx, 409, "OPERATION_NOT_ALLOWED", "Operation not allowed for this reservation");
        } else if (message.equals("CAPACITY_EXCEEDED")) {
            sendError(ctx, 422, "CAPACITY_EXCEEDED", "Number of guests exceeds room capacity");
        } else {
            sendError(ctx, 400, "INVALID_REQUEST", message);
        }
    }

    private static void sendError(Context ctx, int status, String code, String message) {
        ctx.status(status);
        Map<String, Object> error = new HashMap<>();
        error.put("error", code);
        error.put("message", message);
        ctx.json(error);
    }

    private static Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }

    private static String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return value.toString();
    }

    private static LocalDate getDateValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        try {
            return LocalDate.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
