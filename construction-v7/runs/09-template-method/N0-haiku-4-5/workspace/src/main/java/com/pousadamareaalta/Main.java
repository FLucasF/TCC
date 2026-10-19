package com.pousadamareaalta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamareaalta.model.Message;
import com.pousadamareaalta.model.Reservation;
import com.pousadamareaalta.service.PricingService;
import com.pousadamareaalta.service.ReportService;
import com.pousadamareaalta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.NotFoundResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class Main {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ReservationService reservationService = new ReservationService();
    private static final ReportService reportService = new ReportService(reservationService);
    private static final PricingService pricingService = new PricingService();

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Javalin app = Javalin.create()
                .start(port);

        app.get("/rooms", ctx -> {
            ctx.json(reservationService.getRooms().stream().map(room -> new java.util.HashMap<String, Object>() {{
                put("number", room.getNumber());
                put("type", room.getType());
                put("capacity", room.getCapacity());
                put("nightlyRate", formatPrice(room.getNightlyRate()));
            }}).toList());
        });

        app.post("/reservations", ctx -> {
            try {
                JsonNode body = mapper.readTree(ctx.body());

                if (!hasRequiredFields(body, "roomNumber", "guestName", "guestEmail", "guestCpf", "checkIn", "checkOut", "guests", "ratePlan")) {
                    throw new BadRequestResponse("INVALID_REQUEST");
                }

                int roomNumber = body.get("roomNumber").asInt();
                String guestName = body.get("guestName").asText();
                String guestEmail = body.get("guestEmail").asText();
                String guestCpf = body.get("guestCpf").asText();
                LocalDate checkIn = LocalDate.parse(body.get("checkIn").asText(), DateTimeFormatter.ISO_LOCAL_DATE);
                LocalDate checkOut = LocalDate.parse(body.get("checkOut").asText(), DateTimeFormatter.ISO_LOCAL_DATE);
                int guests = body.get("guests").asInt();
                Reservation.RatePlan ratePlan = Reservation.RatePlan.valueOf(body.get("ratePlan").asText());

                Reservation reservation = reservationService.createReservation(roomNumber, guestName, guestEmail,
                        guestCpf, checkIn, checkOut, guests, ratePlan);

                ctx.status(201);
                ctx.json(serializeReservation(reservation));
            } catch (IllegalArgumentException e) {
                handleReservationError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "INVALID_REQUEST");
                    put("message", "Invalid request");
                }});
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    throw new NotFoundResponse("RESERVATION_NOT_FOUND");
                }
                ctx.json(serializeReservation(reservation));
            } catch (NumberFormatException e) {
                ctx.status(404);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "RESERVATION_NOT_FOUND");
                    put("message", "Reservation not found");
                }});
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    throw new BadRequestResponse("RESERVATION_NOT_FOUND");
                }
                reservationService.confirmReservation(id, reservation.getTotal());
                ctx.json(serializeReservation(reservation));
            } catch (NumberFormatException e) {
                ctx.status(404);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "RESERVATION_NOT_FOUND");
                    put("message", "Reservation not found");
                }});
            } catch (IllegalArgumentException e) {
                handleReservationError(ctx, e.getMessage());
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    throw new BadRequestResponse("RESERVATION_NOT_FOUND");
                }
                reservationService.cancelReservation(id);
                ctx.json(serializeReservation(reservation));
            } catch (NumberFormatException e) {
                ctx.status(404);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "RESERVATION_NOT_FOUND");
                    put("message", "Reservation not found");
                }});
            } catch (IllegalArgumentException e) {
                handleReservationError(ctx, e.getMessage());
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    throw new BadRequestResponse("RESERVATION_NOT_FOUND");
                }
                reservationService.checkIn(id);
                ctx.json(serializeReservation(reservation));
            } catch (NumberFormatException e) {
                ctx.status(404);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "RESERVATION_NOT_FOUND");
                    put("message", "Reservation not found");
                }});
            } catch (IllegalArgumentException e) {
                handleReservationError(ctx, e.getMessage());
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    throw new BadRequestResponse("RESERVATION_NOT_FOUND");
                }
                reservationService.checkOut(id);
                ctx.json(generateReceipt(reservation));
            } catch (NumberFormatException e) {
                ctx.status(404);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "RESERVATION_NOT_FOUND");
                    put("message", "Reservation not found");
                }});
            } catch (IllegalArgumentException e) {
                handleReservationError(ctx, e.getMessage());
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    ctx.status(404);
                    ctx.json(new java.util.HashMap<String, Object>() {{
                        put("error", "RESERVATION_NOT_FOUND");
                        put("message", "Reservation not found");
                    }});
                    return;
                }
                if (reservation.getStatus() == Reservation.Status.PENDING) {
                    ctx.status(409);
                    ctx.json(new java.util.HashMap<String, Object>() {{
                        put("error", "OPERATION_NOT_ALLOWED");
                        put("message", "This operation is not allowed for this reservation state");
                    }});
                    return;
                }
                ctx.json(generateReceipt(reservation));
            } catch (NumberFormatException e) {
                ctx.status(404);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "RESERVATION_NOT_FOUND");
                    put("message", "Reservation not found");
                }});
            }
        });

        app.get("/messages", ctx -> {
            ctx.json(reservationService.getMessages().stream().map(msg -> new java.util.HashMap<String, Object>() {{
                put("id", msg.getId());
                put("channel", msg.getChannel().toString());
                put("to", msg.getTo());
                put("subject", msg.getSubject());
                put("body", msg.getBody());
            }}).toList());
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateParam = ctx.queryParam("date");
                if (dateParam == null) {
                    throw new BadRequestResponse("INVALID_REQUEST");
                }
                LocalDate date = LocalDate.parse(dateParam, DateTimeFormatter.ISO_LOCAL_DATE);
                String report = reportService.generateOccupancyReport(date);
                ctx.result(report);
                ctx.contentType("text/csv; charset=utf-8");
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "INVALID_REQUEST");
                    put("message", "Invalid date format");
                }});
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null) {
                    throw new BadRequestResponse("INVALID_REQUEST");
                }
                YearMonth month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = reportService.generateRevenueReport(month);
                ctx.result(report);
                ctx.contentType("text/csv; charset=utf-8");
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "INVALID_REQUEST");
                    put("message", "Invalid month format");
                }});
            }
        });

        app.get("/reports/tourism-tax", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null) {
                    throw new BadRequestResponse("INVALID_REQUEST");
                }
                YearMonth month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = reportService.generateTourismTaxReport(month);
                ctx.result(report);
                ctx.contentType("text/csv; charset=utf-8");
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "INVALID_REQUEST");
                    put("message", "Invalid month format");
                }});
            }
        });

        app.get("/reports/rate-plans", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null) {
                    throw new BadRequestResponse("INVALID_REQUEST");
                }
                YearMonth month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = reportService.generateRatePlansReport(month);
                ctx.result(report);
                ctx.contentType("text/csv; charset=utf-8");
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "INVALID_REQUEST");
                    put("message", "Invalid month format");
                }});
            }
        });
    }

    private static java.util.HashMap<String, Object> serializeReservation(Reservation res) {
        return new java.util.HashMap<String, Object>() {{
            put("id", res.getId());
            put("roomNumber", res.getRoomNumber());
            put("guestName", res.getGuestName());
            put("guestEmail", res.getGuestEmail());
            put("guestCpf", res.getGuestCpf());
            put("checkIn", res.getCheckIn().toString());
            put("checkOut", res.getCheckOut().toString());
            put("guests", res.getGuests());
            put("ratePlan", res.getRatePlan().toString());
            put("nights", res.getNights());
            put("subtotal", formatPrice(res.getSubtotal()));
            put("discount", formatPrice(res.getDiscount()));
            put("total", formatPrice(res.getTotal()));
            put("status", res.getStatus().toString());
            put("amountPaid", formatPrice(res.getAmountPaid()));
            put("refund", res.getRefund() != null ? formatPrice(res.getRefund()) : null);
        }};
    }

    private static java.util.HashMap<String, Object> generateReceipt(Reservation res) {
        return new java.util.HashMap<String, Object>() {{
            put("title", "Comprovante de reserva");
            put("reservationId", res.getId());
            put("guestName", res.getGuestName());
            put("guestCpf", res.getGuestCpf());
            put("roomNumber", res.getRoomNumber());
            put("roomType", reservationService.getRoom(res.getRoomNumber()).getType());
            put("checkIn", res.getCheckIn().toString());
            put("checkOut", res.getCheckOut().toString());
            put("nights", res.getNights());
            put("subtotal", formatPrice(res.getSubtotal()));
            put("discount", formatPrice(res.getDiscount()));
            put("total", formatPrice(res.getTotal()));
        }};
    }

    private static Double formatPrice(BigDecimal value) {
        return value.doubleValue();
    }

    private static boolean hasRequiredFields(JsonNode node, String... fields) {
        for (String field : fields) {
            if (!node.has(field)) {
                return false;
            }
        }
        return true;
    }

    private static void handleReservationError(io.javalin.http.Context ctx, String error) {
        switch (error) {
            case "ROOM_NOT_FOUND" -> {
                ctx.status(404);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "ROOM_NOT_FOUND");
                    put("message", "Room not found");
                }});
            }
            case "ROOM_UNAVAILABLE" -> {
                ctx.status(409);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "ROOM_UNAVAILABLE");
                    put("message", "Room is unavailable for the selected dates");
                }});
            }
            case "CAPACITY_EXCEEDED" -> {
                ctx.status(422);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "CAPACITY_EXCEEDED");
                    put("message", "Number of guests exceeds room capacity");
                }});
            }
            case "OPERATION_NOT_ALLOWED" -> {
                ctx.status(409);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "OPERATION_NOT_ALLOWED");
                    put("message", "This operation is not allowed for this reservation state");
                }});
            }
            default -> {
                ctx.status(400);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "INVALID_REQUEST");
                    put("message", "Invalid request");
                }});
            }
        }
    }
}
