package com.pousada;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pousada.database.Database;
import com.pousada.domain.*;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.report.*;
import com.pousada.service.ApiException;
import com.pousada.service.DocumentService;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class App {
    private static final Gson GSON = new Gson();

    public static void main(String[] args) {
        Database.init();
        RoomRepository.initializeRooms();

        int port = getPort();
        Javalin app = Javalin.create().start(port);

        setupErrorHandling(app);
        setupRoutes(app);

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        return portEnv != null ? Integer.parseInt(portEnv) : 7070;
    }

    private static void setupErrorHandling(Javalin app) {
        app.exception(ApiException.class, (e, ctx) -> {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.code);
            error.addProperty("message", e.getMessage());
            ctx.status(e.statusCode);
            ctx.json(error);
        });

        app.exception(Exception.class, (e, ctx) -> {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INTERNAL_ERROR");
            error.addProperty("message", e.getMessage());
            ctx.status(500);
            ctx.json(error);
        });
    }

    private static void setupRoutes(Javalin app) {
        app.get("/rooms", ctx -> {
            ctx.json(RoomRepository.findAll());
        });

        app.post("/reservations", ctx -> {
            try {
                JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);

                if (!hasRequiredFields(body, "roomNumber", "guestName", "guestEmail", "guestCpf", "checkIn", "checkOut", "guests", "ratePlan")) {
                    throw new ApiException("INVALID_REQUEST", "Missing required fields", 400);
                }

                int roomNumber = body.get("roomNumber").getAsInt();
                String guestName = body.get("guestName").getAsString();
                String guestEmail = body.get("guestEmail").getAsString();
                String guestCpf = body.get("guestCpf").getAsString();
                LocalDate checkIn = LocalDate.parse(body.get("checkIn").getAsString());
                LocalDate checkOut = LocalDate.parse(body.get("checkOut").getAsString());
                int guests = body.get("guests").getAsInt();
                RatePlan ratePlan = RatePlan.valueOf(body.get("ratePlan").getAsString());

                Reservation reservation = ReservationService.createReservation(
                        roomNumber, guestName, guestEmail, guestCpf,
                        checkIn, checkOut, guests, ratePlan
                );

                ctx.status(201);
                ctx.json(toJson(reservation));
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                throw new ApiException("INVALID_REQUEST", "Invalid request body", 400);
            }
        });

        app.get("/reservations/{id}", ctx -> {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation reservation = ReservationService.getReservation(id);
            ctx.json(toJson(reservation));
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                int id = ctx.pathParamAsClass("id", Integer.class).get();
                JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);

                if (!body.has("amountPaid")) {
                    throw new ApiException("INVALID_REQUEST", "Missing amountPaid", 400);
                }

                BigDecimal amountPaid = body.get("amountPaid").getAsBigDecimal();
                Reservation reservation = ReservationService.confirmReservation(id, amountPaid);
                ctx.json(toJson(reservation));
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                throw new ApiException("INVALID_REQUEST", "Invalid request body", 400);
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation reservation = ReservationService.cancelReservation(id);
            ctx.json(toJson(reservation));
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation reservation = ReservationService.checkIn(id);
            ctx.json(toJson(reservation));
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation reservation = ReservationService.checkOut(id);
            Room room = RoomRepository.findByNumber(reservation.roomNumber);
            JsonObject document = new JsonObject();
            document.addProperty("title", "Comprovante de reserva");
            document.addProperty("reservationId", reservation.id);
            document.addProperty("guestName", reservation.guestName);
            document.addProperty("guestCpf", reservation.guestCpf);
            document.addProperty("roomNumber", reservation.roomNumber);
            document.addProperty("roomType", room.type);
            document.addProperty("checkIn", reservation.checkIn.toString());
            document.addProperty("checkOut", reservation.checkOut.toString());
            document.addProperty("nights", reservation.nights);
            document.addProperty("subtotal", formatDecimal(reservation.subtotal));
            document.addProperty("discount", formatDecimal(reservation.discount));
            document.addProperty("total", formatDecimal(reservation.total));
            ctx.json(document);
        });

        app.get("/reservations/{id}/document", ctx -> {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation reservation = ReservationService.getReservation(id);

            if (reservation.status == ReservationStatus.PENDING) {
                throw new ApiException("OPERATION_NOT_ALLOWED", "Document only available for paid reservations", 409);
            }

            Room room = RoomRepository.findByNumber(reservation.roomNumber);
            JsonObject document = new JsonObject();
            document.addProperty("title", "Comprovante de reserva");
            document.addProperty("reservationId", reservation.id);
            document.addProperty("guestName", reservation.guestName);
            document.addProperty("guestCpf", reservation.guestCpf);
            document.addProperty("roomNumber", reservation.roomNumber);
            document.addProperty("roomType", room.type);
            document.addProperty("checkIn", reservation.checkIn.toString());
            document.addProperty("checkOut", reservation.checkOut.toString());
            document.addProperty("nights", reservation.nights);
            document.addProperty("subtotal", formatDecimal(reservation.subtotal));
            document.addProperty("discount", formatDecimal(reservation.discount));
            document.addProperty("total", formatDecimal(reservation.total));
            ctx.json(document);
        });

        app.get("/messages", ctx -> {
            ctx.json(MessageRepository.findAll());
        });

        app.get("/reports/occupancy", ctx -> {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                throw new ApiException("INVALID_REQUEST", "Missing date parameter", 400);
            }

            try {
                LocalDate date = LocalDate.parse(dateStr);
                Report report = new OccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report.generate());
            } catch (Exception e) {
                throw new ApiException("INVALID_REQUEST", "Invalid date format", 400);
            }
        });

        app.get("/reports/revenue", ctx -> {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                throw new ApiException("INVALID_REQUEST", "Missing month parameter", 400);
            }

            try {
                YearMonth month = YearMonth.parse(monthStr);
                Report report = new RevenueReport(month);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report.generate());
            } catch (Exception e) {
                throw new ApiException("INVALID_REQUEST", "Invalid month format", 400);
            }
        });

        app.get("/reports/tourism-tax", ctx -> {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                throw new ApiException("INVALID_REQUEST", "Missing month parameter", 400);
            }

            try {
                YearMonth month = YearMonth.parse(monthStr);
                Report report = new TourismTaxReport(month);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report.generate());
            } catch (Exception e) {
                throw new ApiException("INVALID_REQUEST", "Invalid month format", 400);
            }
        });

        app.get("/reports/rate-plans", ctx -> {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                throw new ApiException("INVALID_REQUEST", "Missing month parameter", 400);
            }

            try {
                YearMonth month = YearMonth.parse(monthStr);
                Report report = new RatePlanReport(month);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report.generate());
            } catch (Exception e) {
                throw new ApiException("INVALID_REQUEST", "Invalid month format", 400);
            }
        });
    }

    private static boolean hasRequiredFields(JsonObject obj, String... fields) {
        for (String field : fields) {
            if (!obj.has(field)) {
                return false;
            }
        }
        return true;
    }

    private static JsonObject toJson(Reservation res) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", res.id);
        obj.addProperty("roomNumber", res.roomNumber);
        obj.addProperty("guestName", res.guestName);
        obj.addProperty("guestEmail", res.guestEmail);
        obj.addProperty("guestCpf", res.guestCpf);
        obj.addProperty("checkIn", res.checkIn.toString());
        obj.addProperty("checkOut", res.checkOut.toString());
        obj.addProperty("guests", res.guests);
        obj.addProperty("ratePlan", res.ratePlan.name());
        obj.addProperty("nights", res.nights);
        obj.addProperty("subtotal", formatDecimal(res.subtotal));
        obj.addProperty("discount", formatDecimal(res.discount));
        obj.addProperty("total", formatDecimal(res.total));
        obj.addProperty("status", res.status.name());
        obj.addProperty("amountPaid", formatDecimal(res.amountPaid));
        obj.addProperty("refund", res.refund != null ? formatDecimal(res.refund) : null);
        return obj;
    }

    private static Number formatDecimal(BigDecimal value) {
        return Double.parseDouble(value.setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString());
    }
}
