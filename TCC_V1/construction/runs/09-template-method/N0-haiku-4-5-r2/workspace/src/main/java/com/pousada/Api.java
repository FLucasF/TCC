package com.pousada;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.pousada.models.Message;
import com.pousada.models.Receipt;
import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.repositories.MessageRepository;
import com.pousada.repositories.RoomRepository;
import com.pousada.services.ApiException;
import com.pousada.services.ReceiptService;
import com.pousada.services.ReportService;
import com.pousada.services.ReservationService;
import com.pousada.utils.DateUtils;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Api {
    private static final Logger logger = LoggerFactory.getLogger(Api.class);
    private static final Gson gson = createGson();

    private static Gson createGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(BigDecimal.class, new JsonSerializer<BigDecimal>() {
            @Override
            public JsonElement serialize(BigDecimal src, Type typeOfSrc,
                                         JsonSerializationContext context) {
                return new JsonPrimitive(src.setScale(2, java.math.RoundingMode.HALF_EVEN));
            }
        });
        return builder.create();
    }

    public static void register(Javalin app) {
        app.get("/rooms", Api::getRooms);

        app.post("/reservations", Api::createReservation);
        app.get("/reservations/{id}", Api::getReservation);
        app.post("/reservations/{id}/confirmation", Api::confirmReservation);
        app.post("/reservations/{id}/check-in", Api::checkIn);
        app.post("/reservations/{id}/check-out", Api::checkOut);
        app.post("/reservations/{id}/cancellation", Api::cancelReservation);
        app.get("/reservations/{id}/document", Api::getDocument);

        app.get("/messages", Api::getMessages);

        app.get("/reports/occupancy", Api::getOccupancyReport);
        app.get("/reports/revenue", Api::getRevenueReport);
        app.get("/reports/tourism-tax", Api::getTourismTaxReport);
        app.get("/reports/rate-plans", Api::getRatePlanReport);

        app.exception(ApiException.class, Api::handleApiException);
        app.exception(IllegalArgumentException.class, Api::handleIllegalArgumentException);
        app.exception(Exception.class, Api::handleGenericException);
    }

    private static void getRooms(Context ctx) {
        List<Room> rooms = RoomRepository.findAll();
        ctx.json(rooms);
    }

    private static void createReservation(Context ctx) {
        try {
            Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

            if (body == null || !body.containsKey("roomNumber") || !body.containsKey("guestName") ||
                !body.containsKey("guestEmail") || !body.containsKey("guestCpf") ||
                !body.containsKey("checkIn") || !body.containsKey("checkOut") ||
                !body.containsKey("guests") || !body.containsKey("ratePlan")) {
                throw new ApiException(400, "INVALID_REQUEST", "Missing required fields");
            }

            ReservationService.CreateReservationRequest req = new ReservationService.CreateReservationRequest();
            req.roomNumber = ((Number) body.get("roomNumber")).intValue();
            req.guestName = (String) body.get("guestName");
            req.guestEmail = (String) body.get("guestEmail");
            req.guestCpf = (String) body.get("guestCpf");
            req.checkIn = DateUtils.parseDate((String) body.get("checkIn"));
            req.checkOut = DateUtils.parseDate((String) body.get("checkOut"));
            req.guests = ((Number) body.get("guests")).intValue();
            req.ratePlan = (String) body.get("ratePlan");

            Reservation reservation = ReservationService.createReservation(req);
            ctx.status(HttpStatus.CREATED);
            ctx.json(toJson(reservation));
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "INVALID_REQUEST", e.getMessage());
        }
    }

    private static void getReservation(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Reservation reservation = com.pousada.repositories.ReservationRepository.findById(id);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }
        ctx.json(toJson(reservation));
    }

    private static void confirmReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

            if (body == null || !body.containsKey("amountPaid")) {
                throw new ApiException(400, "INVALID_REQUEST", "Missing amountPaid field");
            }

            BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());
            Reservation reservation = ReservationService.confirmReservation(id, amountPaid);
            ctx.json(toJson(reservation));
        } catch (NumberFormatException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid amount");
        }
    }

    private static void checkIn(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Reservation reservation = ReservationService.checkIn(id);
        ctx.json(toJson(reservation));
    }

    private static void checkOut(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Reservation reservation = ReservationService.checkOut(id);
        Receipt receipt = ReceiptService.generateReceipt(reservation);
        ctx.json(toReceiptJson(receipt));
    }

    private static void cancelReservation(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Reservation reservation = ReservationService.cancelReservation(id);
        ctx.json(toJson(reservation));
    }

    private static void getDocument(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Reservation reservation = com.pousada.repositories.ReservationRepository.findById(id);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (!("CONFIRMED".equals(reservation.getStatus()) ||
              "CHECKED_IN".equals(reservation.getStatus()) ||
              "CHECKED_OUT".equals(reservation.getStatus()))) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Receipt only available for paid reservations");
        }

        Receipt receipt = ReceiptService.generateReceipt(reservation);
        ctx.json(toReceiptJson(receipt));
    }

    private static void getMessages(Context ctx) {
        List<Message> messages = MessageRepository.findAll();
        ctx.json(messages.stream().map(m -> Map.of(
            "id", m.getId(),
            "channel", m.getChannel(),
            "to", m.getTo(),
            "subject", m.getSubject(),
            "body", m.getBody()
        )).toList());
    }

    private static void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null) {
            throw new ApiException(400, "INVALID_REQUEST", "Missing date parameter");
        }

        try {
            LocalDate date = DateUtils.parseDate(dateStr);
            String report = ReportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid date format");
        }
    }

    private static void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new ApiException(400, "INVALID_REQUEST", "Missing month parameter");
        }

        try {
            YearMonth month = DateUtils.parseMonth(monthStr);
            String report = ReportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid month format");
        }
    }

    private static void getTourismTaxReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new ApiException(400, "INVALID_REQUEST", "Missing month parameter");
        }

        try {
            YearMonth month = DateUtils.parseMonth(monthStr);
            String report = ReportService.generateTourismTaxReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid month format");
        }
    }

    private static void getRatePlanReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new ApiException(400, "INVALID_REQUEST", "Missing month parameter");
        }

        try {
            YearMonth month = DateUtils.parseMonth(monthStr);
            String report = ReportService.generateRatePlanReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid month format");
        }
    }

    private static void handleApiException(ApiException e, Context ctx) {
        ctx.status(e.getStatusCode());
        ctx.json(Map.of(
            "error", e.getErrorCode(),
            "message", e.getMessage()
        ));
    }

    private static void handleIllegalArgumentException(IllegalArgumentException e, Context ctx) {
        ctx.status(400);
        ctx.json(Map.of(
            "error", "INVALID_REQUEST",
            "message", e.getMessage()
        ));
    }

    private static void handleGenericException(Exception e, Context ctx) {
        logger.error("Unexpected error", e);
        ctx.status(500);
        ctx.json(Map.of(
            "error", "INTERNAL_ERROR",
            "message", "An unexpected error occurred"
        ));
    }

    private static Map<String, Object> toJson(Reservation res) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", res.getId());
        map.put("roomNumber", res.getRoomNumber());
        map.put("guestName", res.getGuestName());
        map.put("guestEmail", res.getGuestEmail());
        map.put("guestCpf", res.getGuestCpf());
        map.put("checkIn", DateUtils.formatIsoDate(res.getCheckIn()));
        map.put("checkOut", DateUtils.formatIsoDate(res.getCheckOut()));
        map.put("guests", res.getGuests());
        map.put("ratePlan", res.getRatePlan());
        map.put("nights", res.getNights());
        map.put("subtotal", res.getSubtotal());
        map.put("discount", res.getDiscount());
        map.put("total", res.getTotal());
        map.put("status", res.getStatus());
        map.put("amountPaid", res.getAmountPaid());
        map.put("refund", res.getRefund());
        return map;
    }

    private static Map<String, Object> toReceiptJson(Receipt receipt) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", receipt.getTitle());
        map.put("reservationId", receipt.getReservationId());
        map.put("guestName", receipt.getGuestName());
        map.put("guestCpf", receipt.getGuestCpf());
        map.put("roomNumber", receipt.getRoomNumber());
        map.put("roomType", receipt.getRoomType());
        map.put("checkIn", DateUtils.formatIsoDate(receipt.getCheckIn()));
        map.put("checkOut", DateUtils.formatIsoDate(receipt.getCheckOut()));
        map.put("nights", receipt.getNights());
        map.put("subtotal", receipt.getSubtotal());
        map.put("discount", receipt.getDiscount());
        map.put("total", receipt.getTotal());
        return map;
    }
}
