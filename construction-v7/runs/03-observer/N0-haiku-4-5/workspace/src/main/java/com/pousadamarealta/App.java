package com.pousadamarealta;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import com.pousadamarealta.service.*;
import com.pousadamarealta.model.*;
import java.util.*;

public class App {
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, new com.google.gson.JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT,
                    com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
                return LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_DATE);
            }
        })
        .create();

    public static void main(String[] args) {
        ReservationService service = ReservationService.getInstance();
        Javalin app = Javalin.create();

        app.get("/rooms", ctx -> getRooms(ctx, service));
        app.post("/reservations", ctx -> createReservation(ctx, service));
        app.get("/reservations/{id}", ctx -> getReservation(ctx, service));
        app.post("/reservations/{id}/confirmation", ctx -> confirmReservation(ctx, service));
        app.post("/reservations/{id}/cancellation", ctx -> cancelReservation(ctx, service));
        app.post("/reservations/{id}/check-in", ctx -> checkIn(ctx, service));
        app.post("/reservations/{id}/check-out", ctx -> checkOut(ctx, service));
        app.get("/reservations/{id}/document", ctx -> getDocument(ctx, service));
        app.get("/messages", ctx -> getMessages(ctx, service));
        app.get("/reports/occupancy", ctx -> getOccupancyReport(ctx, service));
        app.get("/reports/revenue", ctx -> getRevenueReport(ctx, service));

        int port = getPort();
        app.start(port);
    }

    private static int getPort() {
        String port = System.getenv("PORT");
        if (port != null && !port.isEmpty()) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                return 7070;
            }
        }
        return 7070;
    }

    private static void getRooms(Context ctx, ReservationService service) {
        List<Map<String, Object>> roomsList = new ArrayList<>();
        for (Room room : service.getRooms()) {
            Map<String, Object> roomMap = new LinkedHashMap<>();
            roomMap.put("number", room.getNumber());
            roomMap.put("type", room.getType().toString());
            roomMap.put("capacity", room.getCapacity());
            roomMap.put("nightlyRate", PricingService.roundHalfToEven(room.getNightlyRate()));
            roomsList.add(roomMap);
        }
        ctx.json(roomsList);
    }

    private static void createReservation(Context ctx, ReservationService service) {
        try {
            Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

            if (!body.containsKey("roomNumber") || !body.containsKey("guestName") ||
                !body.containsKey("guestEmail") || !body.containsKey("guestCpf") ||
                !body.containsKey("checkIn") || !body.containsKey("checkOut") ||
                !body.containsKey("guests") || !body.containsKey("ratePlan")) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Missing required fields"));
                return;
            }

            int roomNumber = ((Number) body.get("roomNumber")).intValue();
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String guestCpf = (String) body.get("guestCpf");
            String checkInStr = (String) body.get("checkIn");
            String checkOutStr = (String) body.get("checkOut");
            int guests = ((Number) body.get("guests")).intValue();
            String ratePlan = (String) body.get("ratePlan");

            try {
                LocalDate checkIn = LocalDate.parse(checkInStr, DateTimeFormatter.ISO_DATE);
                LocalDate checkOut = LocalDate.parse(checkOutStr, DateTimeFormatter.ISO_DATE);

                if (guests < 1) {
                    ctx.status(400);
                    ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Guests must be at least 1"));
                    return;
                }

                if (!checkOut.isAfter(checkIn)) {
                    ctx.status(400);
                    ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Check-out must be after check-in"));
                    return;
                }

                Room room = service.getRooms().stream()
                    .filter(r -> r.getNumber() == roomNumber)
                    .findFirst()
                    .orElse(null);

                if (room == null) {
                    ctx.status(404);
                    ctx.json(Map.of("error", "ROOM_NOT_FOUND", "message", "Room not found"));
                    return;
                }

                if (guests > room.getCapacity()) {
                    ctx.status(422);
                    ctx.json(Map.of("error", "CAPACITY_EXCEEDED", "message", "Room capacity exceeded"));
                    return;
                }

                Reservation res = service.createReservation(roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan);

                if (res == null) {
                    ctx.status(409);
                    ctx.json(Map.of("error", "ROOM_UNAVAILABLE", "message", "Room is not available for these dates"));
                    return;
                }

                ctx.status(201);
                ctx.json(reservationToJson(res));
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
            }
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid request"));
        }
    }

    private static void getReservation(Context ctx, ReservationService service) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = service.getReservation(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                return;
            }

            ctx.json(reservationToJson(res));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
        }
    }

    private static void confirmReservation(Context ctx, ReservationService service) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = service.getReservation(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                return;
            }

            if (res.getStatus() != ReservationStatus.PENDING) {
                ctx.status(409);
                ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Can only confirm a pending reservation"));
                return;
            }

            Reservation confirmed = service.confirmReservation(id, res.getTotal());

            if (confirmed == null) {
                ctx.status(409);
                ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Cannot confirm this reservation"));
                return;
            }

            ctx.json(reservationToJson(confirmed));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
        }
    }

    private static void cancelReservation(Context ctx, ReservationService service) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = service.getReservation(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                return;
            }

            if (res.getStatus() != ReservationStatus.PENDING && res.getStatus() != ReservationStatus.CONFIRMED) {
                ctx.status(409);
                ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Cannot cancel this reservation"));
                return;
            }

            Reservation cancelled = service.cancelReservation(id);

            if (cancelled == null) {
                ctx.status(409);
                ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Cannot cancel this reservation"));
                return;
            }

            ctx.json(reservationToJson(cancelled));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
        }
    }

    private static void checkIn(Context ctx, ReservationService service) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = service.getReservation(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                return;
            }

            if (res.getStatus() != ReservationStatus.CONFIRMED) {
                ctx.status(409);
                ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Can only check in a confirmed reservation"));
                return;
            }

            Reservation checkedIn = service.checkIn(id);

            if (checkedIn == null) {
                ctx.status(409);
                ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Cannot check in this reservation"));
                return;
            }

            ctx.json(reservationToJson(checkedIn));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
        }
    }

    private static void checkOut(Context ctx, ReservationService service) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = service.getReservation(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                return;
            }

            if (res.getStatus() != ReservationStatus.CHECKED_IN) {
                ctx.status(409);
                ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Can only check out a checked-in reservation"));
                return;
            }

            Reservation checkedOut = service.checkOut(id);

            if (checkedOut == null) {
                ctx.status(409);
                ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Cannot check out this reservation"));
                return;
            }

            Room room = service.getRooms().stream()
                .filter(r -> r.getNumber() == checkedOut.getRoomNumber())
                .findFirst()
                .orElse(null);

            Receipt receipt = new Receipt(
                checkedOut.getId(),
                checkedOut.getGuestName(),
                FormatService.formatCPF(checkedOut.getGuestCpf()),
                checkedOut.getRoomNumber(),
                room.getType().toString(),
                FormatService.formatDateBR(checkedOut.getCheckIn()),
                FormatService.formatDateBR(checkedOut.getCheckOut()),
                checkedOut.getNights(),
                checkedOut.getSubtotal(),
                checkedOut.getDiscount(),
                checkedOut.getTotal()
            );

            ctx.json(receiptToJson(receipt));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
        }
    }

    private static void getDocument(Context ctx, ReservationService service) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = service.getReservation(id);

            if (res == null) {
                ctx.status(404);
                ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                return;
            }

            if (res.getStatus() != ReservationStatus.CONFIRMED &&
                res.getStatus() != ReservationStatus.CHECKED_IN &&
                res.getStatus() != ReservationStatus.CHECKED_OUT) {
                ctx.status(404);
                ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Document not available for this reservation"));
                return;
            }

            Room room = service.getRooms().stream()
                .filter(r -> r.getNumber() == res.getRoomNumber())
                .findFirst()
                .orElse(null);

            Receipt receipt = new Receipt(
                res.getId(),
                res.getGuestName(),
                FormatService.formatCPF(res.getGuestCpf()),
                res.getRoomNumber(),
                room.getType().toString(),
                FormatService.formatDateBR(res.getCheckIn()),
                FormatService.formatDateBR(res.getCheckOut()),
                res.getNights(),
                res.getSubtotal(),
                res.getDiscount(),
                res.getTotal()
            );

            ctx.json(receiptToJson(receipt));
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
        }
    }

    private static void getMessages(Context ctx, ReservationService service) {
        List<Map<String, Object>> messagesList = new ArrayList<>();
        for (Message msg : service.getMessages()) {
            Map<String, Object> msgMap = new LinkedHashMap<>();
            msgMap.put("id", msg.getId());
            msgMap.put("channel", msg.getChannel());
            msgMap.put("to", msg.getTo());
            msgMap.put("subject", msg.getSubject());
            msgMap.put("body", msg.getBody());
            messagesList.add(msgMap);
        }
        ctx.json(messagesList);
    }

    private static void getOccupancyReport(Context ctx, ReservationService service) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Date parameter required"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
            String report = service.getOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
        }
    }

    private static void getRevenueReport(Context ctx, ReservationService service) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Month parameter required"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
            String report = service.getRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
        }
    }

    private static Map<String, Object> reservationToJson(Reservation res) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", res.getId());
        json.put("roomNumber", res.getRoomNumber());
        json.put("guestName", res.getGuestName());
        json.put("guestEmail", res.getGuestEmail());
        json.put("guestCpf", res.getGuestCpf());
        json.put("checkIn", FormatService.formatDateISO(res.getCheckIn()));
        json.put("checkOut", FormatService.formatDateISO(res.getCheckOut()));
        json.put("guests", res.getGuests());
        json.put("ratePlan", res.getRatePlan().toString());
        json.put("nights", res.getNights());
        json.put("subtotal", PricingService.roundHalfToEven(res.getSubtotal()));
        json.put("discount", PricingService.roundHalfToEven(res.getDiscount()));
        json.put("total", PricingService.roundHalfToEven(res.getTotal()));
        json.put("status", res.getStatus().toString());
        json.put("amountPaid", PricingService.roundHalfToEven(res.getAmountPaid()));
        json.put("refund", res.getRefund() != null ? PricingService.roundHalfToEven(res.getRefund()) : null);
        return json;
    }

    private static Map<String, Object> receiptToJson(Receipt receipt) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("title", receipt.getTitle());
        json.put("reservationId", receipt.getReservationId());
        json.put("guestName", receipt.getGuestName());
        json.put("guestCpf", receipt.getGuestCpf());
        json.put("roomNumber", receipt.getRoomNumber());
        json.put("roomType", receipt.getRoomType());
        json.put("checkIn", receipt.getCheckIn());
        json.put("checkOut", receipt.getCheckOut());
        json.put("nights", receipt.getNights());
        json.put("subtotal", PricingService.roundHalfToEven(receipt.getSubtotal()));
        json.put("discount", PricingService.roundHalfToEven(receipt.getDiscount()));
        json.put("total", PricingService.roundHalfToEven(receipt.getTotal()));
        return json;
    }
}
