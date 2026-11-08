package com.pousadamarealta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pousadamarealta.api.CreateReservationRequest;
import com.pousadamarealta.api.ErrorResponse;
import com.pousadamarealta.db.Database;
import com.pousadamarealta.db.MessageRepository;
import com.pousadamarealta.db.RoomRepository;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.notification.FinanceNotifier;
import com.pousadamarealta.notification.HousekeepingNotifier;
import com.pousadamarealta.notification.ReservationEventDispatcher;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.ReportService;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Main {
    static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
        .registerTypeAdapter(BigDecimal.class, new BigDecimalTypeAdapter())
        .create();

    public static void main(String[] args) throws SQLException {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Database.init(dbUrl, dbUser, dbPassword);

        ReservationEventDispatcher.addListener(new HousekeepingNotifier());
        ReservationEventDispatcher.addListener(new FinanceNotifier());

        Javalin app = Javalin.create()
            .get("/rooms", ctx -> {
                try {
                    var rooms = RoomRepository.findAll();
                    ctx.json(rooms);
                } catch (Exception e) {
                    ctx.status(500).json(new ErrorResponse("ERROR", e.getMessage()));
                }
            })
            .post("/reservations", ctx -> {
                try {
                    var body = ctx.body();
                    var req = gson.fromJson(body, CreateReservationRequest.class);

                    if (req.roomNumber == 0 || req.guestName == null || req.guestEmail == null ||
                        req.guestCpf == null || req.checkIn == null || req.checkOut == null ||
                        req.guests == 0 || req.ratePlan == null) {
                        ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                        return;
                    }

                    LocalDate checkIn = LocalDate.parse(req.checkIn);
                    LocalDate checkOut = LocalDate.parse(req.checkOut);
                    RatePlan ratePlan = RatePlan.valueOf(req.ratePlan);

                    var reservation = ReservationService.createReservation(
                        req.roomNumber, req.guestName, req.guestEmail, req.guestCpf,
                        checkIn, checkOut, req.guests, ratePlan);

                    ctx.status(201).json(reservationToJson(reservation));
                } catch (IllegalArgumentException e) {
                    handleError(ctx, e.getMessage());
                } catch (Exception e) {
                    ctx.status(500).json(new ErrorResponse("ERROR", e.getMessage()));
                }
            })
            .get("/reservations/{id}", ctx -> {
                try {
                    long id = Long.parseLong(ctx.pathParam("id"));
                    var reservation = ReservationService.getReservation(id);
                    if (reservation.isEmpty()) {
                        ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    } else {
                        ctx.json(reservationToJson(reservation.get()));
                    }
                } catch (Exception e) {
                    ctx.status(500).json(new ErrorResponse("ERROR", e.getMessage()));
                }
            })
            .post("/reservations/{id}/confirmation", ctx -> {
                try {
                    long id = Long.parseLong(ctx.pathParam("id"));
                    Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);
                    BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());

                    var reservation = ReservationService.confirmReservation(id, amountPaid);
                    ctx.json(reservationToJson(reservation));
                } catch (IllegalArgumentException e) {
                    handleError(ctx, e.getMessage());
                } catch (Exception e) {
                    ctx.status(500).json(new ErrorResponse("ERROR", e.getMessage()));
                }
            })
            .post("/reservations/{id}/cancellation", ctx -> {
                try {
                    long id = Long.parseLong(ctx.pathParam("id"));
                    var reservation = ReservationService.cancelReservation(id);
                    ctx.json(reservationToJson(reservation));
                } catch (IllegalArgumentException e) {
                    handleError(ctx, e.getMessage());
                } catch (Exception e) {
                    ctx.status(500).json(new ErrorResponse("ERROR", e.getMessage()));
                }
            })
            .post("/reservations/{id}/check-in", ctx -> {
                try {
                    long id = Long.parseLong(ctx.pathParam("id"));
                    var reservation = ReservationService.checkIn(id);
                    ctx.json(reservationToJson(reservation));
                } catch (IllegalArgumentException e) {
                    handleError(ctx, e.getMessage());
                } catch (Exception e) {
                    ctx.status(500).json(new ErrorResponse("ERROR", e.getMessage()));
                }
            })
            .post("/reservations/{id}/check-out", ctx -> {
                try {
                    long id = Long.parseLong(ctx.pathParam("id"));
                    var reservation = ReservationService.checkOut(id);
                    var document = ReservationService.generateDocument(reservation);
                    ctx.json(documentToJson(reservation, document));
                } catch (IllegalArgumentException e) {
                    handleError(ctx, e.getMessage());
                } catch (Exception e) {
                    ctx.status(500).json(new ErrorResponse("ERROR", e.getMessage()));
                }
            })
            .get("/reservations/{id}/document", ctx -> {
                try {
                    long id = Long.parseLong(ctx.pathParam("id"));
                    var reservation = ReservationService.getReservation(id);
                    if (reservation.isEmpty()) {
                        ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    } else {
                        var res = reservation.get();
                        if (res.amountPaid.signum() <= 0) {
                            ctx.status(422).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "No paid reservation"));
                        } else {
                            var document = ReservationService.generateDocument(res);
                            ctx.json(documentToJson(res, document));
                        }
                    }
                } catch (Exception e) {
                    ctx.status(500).json(new ErrorResponse("ERROR", e.getMessage()));
                }
            })
            .get("/messages", ctx -> {
                try {
                    var messages = MessageRepository.findAll();
                    ctx.json(messages);
                } catch (Exception e) {
                    ctx.status(500).json(new ErrorResponse("ERROR", e.getMessage()));
                }
            })
            .get("/reports/occupancy", ctx -> {
                try {
                    String dateStr = ctx.queryParam("date");
                    if (dateStr == null) {
                        ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                        return;
                    }
                    LocalDate date = LocalDate.parse(dateStr);
                    String report = ReportService.generateOccupancyReport(date);
                    ctx.contentType("text/csv;charset=utf-8").result(report);
                } catch (Exception e) {
                    ctx.status(500).json(new ErrorResponse("ERROR", e.getMessage()));
                }
            })
            .get("/reports/revenue", ctx -> {
                try {
                    String monthStr = ctx.queryParam("month");
                    if (monthStr == null) {
                        ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                        return;
                    }
                    LocalDate monthStart = LocalDate.parse(monthStr + "-01");
                    String report = ReportService.generateRevenueReport(monthStart);
                    ctx.contentType("text/csv;charset=utf-8").result(report);
                } catch (Exception e) {
                    ctx.status(500).json(new ErrorResponse("ERROR", e.getMessage()));
                }
            });

        app.start(port);
        System.out.println("Server running on port " + port);
    }

    private static Map<String, Object> reservationToJson(com.pousadamarealta.model.Reservation res) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", res.id);
        map.put("roomNumber", res.roomNumber);
        map.put("guestName", res.guestName);
        map.put("guestEmail", res.guestEmail);
        map.put("guestCpf", res.guestCpf);
        map.put("checkIn", res.checkIn);
        map.put("checkOut", res.checkOut);
        map.put("guests", res.guests);
        map.put("ratePlan", res.ratePlan.toString());
        map.put("nights", res.nights);
        map.put("subtotal", res.subtotal);
        map.put("discount", res.discount);
        map.put("total", res.total);
        map.put("status", res.status.toString());
        map.put("amountPaid", res.amountPaid);
        map.put("refund", res.refund);
        return map;
    }

    private static Map<String, Object> documentToJson(com.pousadamarealta.model.Reservation res, String document) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", res.id);
        map.put("guestName", res.guestName);
        map.put("guestCpf", res.guestCpf);
        map.put("roomNumber", res.roomNumber);
        map.put("roomType", getRoomType(res.roomNumber));
        map.put("checkIn", res.checkIn);
        map.put("checkOut", res.checkOut);
        map.put("nights", res.nights);
        map.put("subtotal", res.subtotal);
        map.put("discount", res.discount);
        map.put("total", res.total);
        return map;
    }

    private static String getRoomType(int roomNumber) {
        try {
            return RoomRepository.findByNumber(roomNumber)
                .map(r -> r.type().toString())
                .orElse("UNKNOWN");
        } catch (SQLException e) {
            return "UNKNOWN";
        }
    }

    private static void handleError(io.javalin.http.Context ctx, String errorCode) {
        switch (errorCode) {
            case "ROOM_NOT_FOUND":
                ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                break;
            case "RESERVATION_NOT_FOUND":
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                break;
            case "ROOM_UNAVAILABLE":
                ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room unavailable for those dates"));
                break;
            case "OPERATION_NOT_ALLOWED":
                ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Operation not allowed in current state"));
                break;
            case "CAPACITY_EXCEEDED":
                ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Too many guests for this room"));
                break;
            default:
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", errorCode));
        }
    }
}

class LocalDateTypeAdapter implements com.google.gson.JsonSerializer<LocalDate>,
        com.google.gson.JsonDeserializer<LocalDate> {
    @Override
    public com.google.gson.JsonElement serialize(LocalDate src, java.lang.reflect.Type typeOfSrc,
            com.google.gson.JsonSerializationContext context) {
        return new com.google.gson.JsonPrimitive(src.toString());
    }

    @Override
    public LocalDate deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT,
            com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
        return LocalDate.parse(json.getAsString());
    }
}

class BigDecimalTypeAdapter implements com.google.gson.JsonSerializer<BigDecimal>,
        com.google.gson.JsonDeserializer<BigDecimal> {
    @Override
    public com.google.gson.JsonElement serialize(BigDecimal src, java.lang.reflect.Type typeOfSrc,
            com.google.gson.JsonSerializationContext context) {
        return new com.google.gson.JsonPrimitive(src);
    }

    @Override
    public BigDecimal deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT,
            com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
        return json.getAsBigDecimal();
    }
}
