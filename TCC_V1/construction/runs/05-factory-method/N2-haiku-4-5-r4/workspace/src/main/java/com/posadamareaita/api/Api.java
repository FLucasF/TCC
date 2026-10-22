package com.posadamareaita.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.posadamareaita.domain.Reservation;
import com.posadamareaita.domain.Room;
import com.posadamareaita.repository.MessageRepository;
import com.posadamareaita.repository.ReservationRepository;
import com.posadamareaita.repository.RoomRepository;
import com.posadamareaita.service.DocumentService;
import com.posadamareaita.service.ReportService;
import com.posadamareaita.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Api {
    private static final Gson gson = new Gson();
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void setupRoutes(Javalin app) {
        app.get("/rooms", Api::getRooms);
        app.post("/reservations", Api::createReservation);
        app.get("/reservations/{id}", Api::getReservation);
        app.post("/reservations/{id}/confirmation", Api::confirmReservation);
        app.post("/reservations/{id}/cancellation", Api::cancelReservation);
        app.post("/reservations/{id}/check-in", Api::checkInReservation);
        app.post("/reservations/{id}/check-out", Api::checkOutReservation);
        app.get("/reservations/{id}/document", Api::getDocument);
        app.get("/messages", Api::getMessages);
        app.get("/reports/occupancy", Api::getOccupancyReport);
        app.get("/reports/revenue", Api::getRevenueReport);
    }

    private static void getRooms(Context ctx) throws Exception {
        List<Room> rooms = RoomRepository.findAll();
        ctx.json(rooms.stream().map(r -> new HashMap<String, Object>() {{
            put("number", r.getNumber());
            put("type", r.getType().getDisplayName());
            put("capacity", r.getCapacity());
            put("nightlyRate", r.getNightlyRate());
        }}).toList());
    }

    private static void createReservation(Context ctx) throws Exception {
        try {
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

            // Validate required fields
            if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
                !body.has("checkIn") || !body.has("checkOut") || !body.has("guests") || !body.has("ratePlan")) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
                return;
            }

            int roomNumber = body.get("roomNumber").getAsInt();
            String guestName = body.get("guestName").getAsString();
            String guestEmail = body.get("guestEmail").getAsString();

            LocalDate checkIn = LocalDate.parse(body.get("checkIn").getAsString(), dateFormatter);
            LocalDate checkOut = LocalDate.parse(body.get("checkOut").getAsString(), dateFormatter);

            int guests = body.get("guests").getAsInt();
            String ratePlanStr = body.get("ratePlan").getAsString();

            // Validate rate plan
            try {
                com.posadamareaita.domain.RatePlan.valueOf(ratePlanStr);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
                return;
            }

            // Get identification fields
            String guestCpf = body.has("guestCpf") && !body.get("guestCpf").isJsonNull() ? body.get("guestCpf").getAsString() : null;
            String companyCnpj = body.has("companyCnpj") && !body.get("companyCnpj").isJsonNull() ? body.get("companyCnpj").getAsString() : null;
            String companyName = body.has("companyName") && !body.get("companyName").isJsonNull() ? body.get("companyName").getAsString() : null;
            String guestPassport = body.has("guestPassport") && !body.get("guestPassport").isJsonNull() ? body.get("guestPassport").getAsString() : null;
            String guestCountry = body.has("guestCountry") && !body.get("guestCountry").isJsonNull() ? body.get("guestCountry").getAsString() : null;

            Optional<Reservation> result = ReservationService.createReservation(
                    roomNumber, guestName, guestEmail, checkIn, checkOut, guests, ratePlanStr,
                    guestCpf, companyCnpj, companyName, guestPassport, guestCountry
            );

            if (result.isEmpty()) {
                // Check what went wrong
                Optional<Room> room = RoomRepository.findByNumber(roomNumber);
                if (room.isEmpty()) {
                    ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                } else if (guests > room.get().getCapacity()) {
                    ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Too many guests for this room"));
                } else if (guests < 1 || !checkOut.isAfter(checkIn)) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request data"));
                } else {
                    ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for these dates"));
                }
                return;
            }

            ctx.status(201).json(buildReservationResponse(result.get()));
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    private static void getReservation(Context ctx) throws Exception {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> res = ReservationRepository.findById(id);

            if (res.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            ctx.json(buildReservationResponse(res.get()));
        } catch (Exception e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    private static void confirmReservation(Context ctx) throws Exception {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

            if (!body.has("amountPaid")) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing amountPaid"));
                return;
            }

            BigDecimal amountPaid = body.get("amountPaid").getAsBigDecimal();
            Optional<Reservation> result = ReservationService.confirm(id, amountPaid);

            if (result.isEmpty()) {
                Optional<Reservation> res = ReservationRepository.findById(id);
                if (res.isEmpty()) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot confirm this reservation"));
                }
                return;
            }

            ctx.json(buildReservationResponse(result.get()));
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    private static void cancelReservation(Context ctx) throws Exception {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> result = ReservationService.cancel(id);

            if (result.isEmpty()) {
                Optional<Reservation> res = ReservationRepository.findById(id);
                if (res.isEmpty()) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot cancel this reservation"));
                }
                return;
            }

            ctx.json(buildReservationResponse(result.get()));
        } catch (Exception e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    private static void checkInReservation(Context ctx) throws Exception {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> result = ReservationService.checkIn(id);

            if (result.isEmpty()) {
                Optional<Reservation> res = ReservationRepository.findById(id);
                if (res.isEmpty()) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check in this reservation"));
                }
                return;
            }

            ctx.json(buildReservationResponse(result.get()));
        } catch (Exception e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    private static void checkOutReservation(Context ctx) throws Exception {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> result = ReservationService.checkOut(id);

            if (result.isEmpty()) {
                Optional<Reservation> res = ReservationRepository.findById(id);
                if (res.isEmpty()) {
                    ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                } else {
                    ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "Cannot check out this reservation"));
                }
                return;
            }

            ctx.json(buildReservationResponse(result.get()));
        } catch (Exception e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    private static void getDocument(Context ctx) throws Exception {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> opt = ReservationService.getDocument(id);

            if (opt.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                return;
            }

            Reservation res = opt.get();
            Optional<Room> room = RoomRepository.findByNumber(res.getRoomNumber());
            if (room.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
                return;
            }

            ctx.json(DocumentService.generateDocumentResponse(res, room.get()));
        } catch (Exception e) {
            ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        }
    }

    private static void getMessages(Context ctx) throws Exception {
        List<com.posadamareaita.domain.Message> messages = MessageRepository.findAll();
        ctx.json(messages.stream().map(m -> new HashMap<String, Object>() {{
            put("id", m.getId());
            put("channel", m.getChannel());
            put("to", m.getTo());
            put("subject", m.getSubject());
            put("body", m.getBody());
        }}).toList());
    }

    private static void getOccupancyReport(Context ctx) throws Exception {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr, dateFormatter);
            String report = ReportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    private static void getRevenueReport(Context ctx) throws Exception {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth ym = YearMonth.parse(monthStr);
            String report = ReportService.generateRevenueReport(ym.getYear(), ym.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    private static Map<String, Object> buildReservationResponse(Reservation res) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", res.getId());
        response.put("roomNumber", res.getRoomNumber());
        response.put("guestName", res.getGuestName());
        response.put("guestEmail", res.getGuestEmail());
        response.put("checkIn", res.getCheckIn().toString());
        response.put("checkOut", res.getCheckOut().toString());
        response.put("guests", res.getGuests());
        response.put("ratePlan", res.getRatePlan().toString());
        response.put("nights", res.getNights());
        response.put("subtotal", res.getSubtotal());
        response.put("discount", res.getDiscount());
        response.put("total", res.getTotal());
        response.put("status", res.getStatus().toString());
        response.put("amountPaid", res.getAmountPaid());
        response.put("refund", res.getRefund());
        response.put("guestCpf", res.getGuestCpf());
        response.put("companyCnpj", res.getCompanyCnpj());
        response.put("companyName", res.getCompanyName());
        response.put("guestPassport", res.getGuestPassport());
        response.put("guestCountry", res.getGuestCountry());
        return response;
    }
}
