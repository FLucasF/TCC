package com.pousadamarealta.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.Javalin;
import io.javalin.http.Context;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.service.*;
import com.pousadamarealta.util.PricingUtil;
import com.pousadamarealta.util.ValidationUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class HotelAPI {
    public static void setup(Javalin app) {
        app.get("/rooms", HotelAPI::listRooms);
        app.post("/reservations", HotelAPI::createReservation);
        app.get("/reservations/{id}", HotelAPI::getReservation);
        app.post("/reservations/{id}/confirmation", HotelAPI::confirmReservation);
        app.post("/reservations/{id}/check-in", HotelAPI::checkIn);
        app.post("/reservations/{id}/check-out", HotelAPI::checkOut);
        app.post("/reservations/{id}/cancellation", HotelAPI::cancelReservation);
        app.get("/reservations/{id}/document", HotelAPI::getDocument);
        app.get("/messages", HotelAPI::getMessages);
        app.get("/reports/occupancy", HotelAPI::getOccupancyReport);
        app.get("/reports/revenue", HotelAPI::getRevenueReport);
    }

    private static void listRooms(Context ctx) {
        try {
            List<Room> rooms = RoomService.getAllRooms();
            ctx.json(rooms.stream().map(r -> new Object() {
                public int number = r.number;
                public String type = r.type;
                public int capacity = r.capacity;
                public BigDecimal nightlyRate = r.nightlyRate;
            }).toList());
        } catch (Exception e) {
            error(ctx, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private static void createReservation(Context ctx) {
        try {
            JsonNode body = ctx.bodyAsClass(JsonNode.class);

            if (!hasRequiredFields(body, "roomNumber", "guestName", "guestEmail", "guestCpf",
                                   "checkIn", "checkOut", "guests", "ratePlan")) {
                error(ctx, 400, "INVALID_REQUEST", "Missing required fields");
                return;
            }

            int roomNumber = body.get("roomNumber").asInt();
            String guestName = body.get("guestName").asText();
            String guestEmail = body.get("guestEmail").asText();
            String guestCpf = body.get("guestCpf").asText();
            int guests = body.get("guests").asInt();
            String ratePlan = body.get("ratePlan").asText();

            LocalDate checkIn = ValidationUtil.parseDate(body.get("checkIn").asText());
            LocalDate checkOut = ValidationUtil.parseDate(body.get("checkOut").asText());

            if (checkIn == null || checkOut == null) {
                error(ctx, 400, "INVALID_REQUEST", "Invalid date format");
                return;
            }

            if (!ValidationUtil.isValidDateRange(checkIn, checkOut)) {
                error(ctx, 400, "INVALID_REQUEST", "Check-out must be after check-in");
                return;
            }

            Room room = RoomService.getRoom(roomNumber);
            if (room == null) {
                error(ctx, 404, "ROOM_NOT_FOUND", "Room not found");
                return;
            }

            if (guests < 1) {
                error(ctx, 400, "INVALID_REQUEST", "At least one guest required");
                return;
            }

            if (guests > room.capacity) {
                error(ctx, 422, "CAPACITY_EXCEEDED", "Too many guests for this room");
                return;
            }

            if (!ReservationService.isRoomAvailable(roomNumber, checkIn, checkOut)) {
                error(ctx, 409, "ROOM_UNAVAILABLE", "Room is not available for these dates");
                return;
            }

            Reservation reservation = ReservationService.create(roomNumber, guestName, guestEmail, guestCpf,
                                                                 checkIn, checkOut, guests, ratePlan);
            ctx.status(201);
            ctx.json(formatReservation(reservation));
        } catch (Exception e) {
            error(ctx, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private static void getReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = ReservationService.getReservation(id);

            if (res == null) {
                error(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
                return;
            }

            ctx.json(formatReservation(res));
        } catch (Exception e) {
            error(ctx, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private static void confirmReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            JsonNode body = ctx.bodyAsClass(JsonNode.class);

            Reservation res = ReservationService.getReservation(id);
            if (res == null) {
                error(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
                return;
            }

            res = ReservationService.confirm(id, res.total);

            Room room = RoomService.getRoom(res.roomNumber);
            res.roomType = room.type;

            String receipt = DocumentService.generateReceipt(res);
            MessageService.sendReservationConfirmation(id, res.guestName, res.guestEmail, receipt);

            ctx.json(formatReservation(res));
        } catch (IllegalStateException e) {
            error(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        } catch (Exception e) {
            error(ctx, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private static void checkIn(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = ReservationService.getReservation(id);

            if (res == null) {
                error(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
                return;
            }

            res = ReservationService.checkIn(id);
            ctx.json(formatReservation(res));
        } catch (IllegalStateException e) {
            error(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        } catch (Exception e) {
            error(ctx, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private static void checkOut(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = ReservationService.getReservation(id);

            if (res == null) {
                error(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
                return;
            }

            res = ReservationService.checkOut(id);
            Room room = RoomService.getRoom(res.roomNumber);
            res.roomType = room.type;

            DocumentService.ReceiptResponse receipt = new DocumentService.ReceiptResponse(res);
            ctx.json(receipt);
        } catch (IllegalStateException e) {
            error(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        } catch (Exception e) {
            error(ctx, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private static void cancelReservation(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = ReservationService.getReservation(id);

            if (res == null) {
                error(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
                return;
            }

            res = ReservationService.cancel(id);
            ctx.json(formatReservation(res));
        } catch (IllegalStateException e) {
            error(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        } catch (Exception e) {
            error(ctx, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private static void getDocument(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation res = ReservationService.getReservation(id);

            if (res == null) {
                error(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
                return;
            }

            if (res.amountPaid == null || res.amountPaid.compareTo(BigDecimal.ZERO) == 0) {
                error(ctx, 409, "OPERATION_NOT_ALLOWED", "No receipt available for unpaid reservations");
                return;
            }

            Room room = RoomService.getRoom(res.roomNumber);
            res.roomType = room.type;

            DocumentService.ReceiptResponse receipt = new DocumentService.ReceiptResponse(res);
            ctx.json(receipt);
        } catch (Exception e) {
            error(ctx, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private static void getMessages(Context ctx) {
        try {
            ctx.json(MessageService.getAllMessages());
        } catch (Exception e) {
            error(ctx, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private static void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                error(ctx, 400, "INVALID_REQUEST", "Missing date parameter");
                return;
            }

            LocalDate date = ValidationUtil.parseDate(dateStr);
            if (date == null) {
                error(ctx, 400, "INVALID_REQUEST", "Invalid date format");
                return;
            }

            String csv = ReportService.getOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (Exception e) {
            error(ctx, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private static void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                error(ctx, 400, "INVALID_REQUEST", "Missing month parameter");
                return;
            }

            YearMonth month = ValidationUtil.parseYearMonth(monthStr);
            if (month == null) {
                error(ctx, 400, "INVALID_REQUEST", "Invalid month format");
                return;
            }

            String csv = ReportService.getRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (Exception e) {
            error(ctx, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private static Object formatReservation(Reservation res) throws Exception {
        return new Object() {
            public int id = res.id;
            public int roomNumber = res.roomNumber;
            public String guestName = res.guestName;
            public String guestEmail = res.guestEmail;
            public String guestCpf = res.guestCpf;
            public String checkIn = res.checkIn.toString();
            public String checkOut = res.checkOut.toString();
            public int guests = res.guests;
            public String ratePlan = res.ratePlan;
            public int nights = res.nights;
            public BigDecimal subtotal = res.subtotal;
            public BigDecimal discount = res.discount;
            public BigDecimal total = res.total;
            public String status = res.status;
            public BigDecimal amountPaid = res.amountPaid;
            public BigDecimal refund = res.refund;
        };
    }

    private static void error(Context ctx, int status, String code, String message) {
        ctx.status(status);
        final String finalCode = code;
        final String finalMessage = message;
        ctx.json(new Object() {
            public String error = finalCode;
            public String message = finalMessage;
        });
    }

    private static boolean hasRequiredFields(JsonNode body, String... fields) {
        for (String field : fields) {
            if (!body.has(field) || body.get(field).isNull()) {
                return false;
            }
        }
        return true;
    }
}
