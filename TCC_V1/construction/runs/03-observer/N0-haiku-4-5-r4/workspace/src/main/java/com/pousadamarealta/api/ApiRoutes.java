package com.pousadamarealta.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pousadamarealta.exceptions.ApiException;
import com.pousadamarealta.models.*;
import com.pousadamarealta.repositories.MessageRepository;
import com.pousadamarealta.repositories.ReservationRepository;
import com.pousadamarealta.repositories.RoomRepository;
import com.pousadamarealta.services.*;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class ApiRoutes {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final ReportService reportService;
    private final Gson gson;

    public ApiRoutes(
        RoomRepository roomRepository,
        ReservationRepository reservationRepository,
        MessageRepository messageRepository,
        ReservationService reservationService,
        NotificationService notificationService,
        ReportService reportService,
        Gson gson
    ) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.messageRepository = messageRepository;
        this.reservationService = reservationService;
        this.notificationService = notificationService;
        this.reportService = reportService;
        this.gson = gson;
    }

    public void registerRoutes(Javalin app) {
        app.get("/rooms", this::getRooms);
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkIn);
        app.post("/reservations/{id}/check-out", this::checkOut);
        app.get("/reservations/{id}/document", this::getDocument);
        app.get("/messages", this::getMessages);
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);
    }

    private void getRooms(Context ctx) {
        try {
            var rooms = roomRepository.findAll().stream()
                .map(room -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("number", room.number());
                    json.addProperty("type", room.type().toString());
                    json.addProperty("capacity", room.capacity());
                    json.addProperty("nightlyRate", room.nightlyRate().doubleValue());
                    return json;
                })
                .collect(Collectors.toList());
            ctx.json(rooms);
        } catch (Exception e) {
            errorResponse(ctx, "INVALID_REQUEST", "Erro ao buscar quartos", 400);
        }
    }

    private void createReservation(Context ctx) {
        try {
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);
            if (body == null) {
                errorResponse(ctx, "INVALID_REQUEST", "Corpo da requisição inválido", 400);
                return;
            }

            validateRequiredFields(body, "roomNumber", "guestName", "guestEmail", "guestCpf",
                "checkIn", "checkOut", "guests", "ratePlan");

            int roomNumber = body.get("roomNumber").getAsInt();
            String guestName = body.get("guestName").getAsString();
            String guestEmail = body.get("guestEmail").getAsString();
            String guestCpf = body.get("guestCpf").getAsString();
            LocalDate checkIn = LocalDate.parse(body.get("checkIn").getAsString());
            LocalDate checkOut = LocalDate.parse(body.get("checkOut").getAsString());
            int guests = body.get("guests").getAsInt();
            RatePlan ratePlan = RatePlan.valueOf(body.get("ratePlan").getAsString());

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201);
            ctx.json(reservationToJson(reservation));
        } catch (ApiException e) {
            errorResponse(ctx, e.getErrorCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            errorResponse(ctx, "INVALID_REQUEST", "Erro ao criar reserva: " + e.getMessage(), 400);
        }
    }

    private void getReservation(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation reservation = reservationService.getReservation(id);
            ctx.json(reservationToJson(reservation));
        } catch (ApiException e) {
            errorResponse(ctx, e.getErrorCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            errorResponse(ctx, "INVALID_REQUEST", "Erro ao buscar reserva", 400);
        }
    }

    private void confirmReservation(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation reservation = reservationService.getReservation(id);

            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);
            if (body == null || !body.has("amountPaid")) {
                errorResponse(ctx, "INVALID_REQUEST", "Campo amountPaid não fornecido", 400);
                return;
            }

            BigDecimal amountPaid = body.get("amountPaid").getAsBigDecimal();
            Reservation confirmed = reservationService.confirmReservation(id, amountPaid);
            ctx.json(reservationToJson(confirmed));
        } catch (ApiException e) {
            errorResponse(ctx, e.getErrorCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            errorResponse(ctx, "INVALID_REQUEST", "Erro ao confirmar reserva", 400);
        }
    }

    private void cancelReservation(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation cancelled = reservationService.cancelReservation(id);
            ctx.json(reservationToJson(cancelled));
        } catch (ApiException e) {
            errorResponse(ctx, e.getErrorCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            errorResponse(ctx, "INVALID_REQUEST", "Erro ao cancelar reserva", 400);
        }
    }

    private void checkIn(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation checked = reservationService.checkIn(id);
            ctx.json(reservationToJson(checked));
        } catch (ApiException e) {
            errorResponse(ctx, e.getErrorCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            errorResponse(ctx, "INVALID_REQUEST", "Erro ao fazer check-in", 400);
        }
    }

    private void checkOut(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation checked = reservationService.checkOut(id);

            JsonObject receipt = new JsonObject();
            receipt.addProperty("title", "Comprovante de reserva");
            receipt.addProperty("reservationId", checked.getId());
            receipt.addProperty("guestName", checked.getGuestName());
            receipt.addProperty("guestCpf", checked.getGuestCpf());
            receipt.addProperty("roomNumber", checked.getRoomNumber());
            receipt.addProperty("roomType", getRoomType(checked.getRoomNumber()));
            receipt.addProperty("checkIn", checked.getCheckIn().toString());
            receipt.addProperty("checkOut", checked.getCheckOut().toString());
            receipt.addProperty("nights", checked.getNights());
            receipt.addProperty("subtotal", checked.getSubtotal().doubleValue());
            receipt.addProperty("discount", checked.getDiscount().doubleValue());
            receipt.addProperty("total", checked.getTotal().doubleValue());

            ctx.json(receipt);
        } catch (ApiException e) {
            errorResponse(ctx, e.getErrorCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            errorResponse(ctx, "INVALID_REQUEST", "Erro ao fazer check-out", 400);
        }
    }

    private void getDocument(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Reservation reservation = reservationService.getReservation(id);

            if (reservation.getStatus() == ReservationStatus.PENDING) {
                errorResponse(ctx, "OPERATION_NOT_ALLOWED",
                    "Comprovante não disponível para reserva pendente", 409);
                return;
            }

            JsonObject receipt = new JsonObject();
            receipt.addProperty("title", "Comprovante de reserva");
            receipt.addProperty("reservationId", reservation.getId());
            receipt.addProperty("guestName", reservation.getGuestName());
            receipt.addProperty("guestCpf", reservation.getGuestCpf());
            receipt.addProperty("roomNumber", reservation.getRoomNumber());
            receipt.addProperty("roomType", getRoomType(reservation.getRoomNumber()));
            receipt.addProperty("checkIn", reservation.getCheckIn().toString());
            receipt.addProperty("checkOut", reservation.getCheckOut().toString());
            receipt.addProperty("nights", reservation.getNights());
            receipt.addProperty("subtotal", reservation.getSubtotal().doubleValue());
            receipt.addProperty("discount", reservation.getDiscount().doubleValue());
            receipt.addProperty("total", reservation.getTotal().doubleValue());

            ctx.json(receipt);
        } catch (ApiException e) {
            errorResponse(ctx, e.getErrorCode(), e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            errorResponse(ctx, "INVALID_REQUEST", "Erro ao buscar comprovante", 400);
        }
    }

    private void getMessages(Context ctx) {
        try {
            var messages = messageRepository.findAll().stream()
                .map(msg -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", msg.id());
                    json.addProperty("channel", msg.channel());
                    json.addProperty("to", msg.to());
                    json.addProperty("subject", msg.subject());
                    json.addProperty("body", msg.body());
                    return json;
                })
                .collect(Collectors.toList());
            ctx.json(messages);
        } catch (Exception e) {
            errorResponse(ctx, "INVALID_REQUEST", "Erro ao buscar mensagens", 400);
        }
    }

    private void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                errorResponse(ctx, "INVALID_REQUEST", "Parâmetro 'date' obrigatório", 400);
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String report = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            errorResponse(ctx, "INVALID_REQUEST", "Erro ao gerar relatório de ocupação", 400);
        }
    }

    private void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                errorResponse(ctx, "INVALID_REQUEST", "Parâmetro 'month' obrigatório", 400);
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthStr);
            String report = reportService.generateRevenueReport(yearMonth);
            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            errorResponse(ctx, "INVALID_REQUEST", "Erro ao gerar relatório financeiro", 400);
        }
    }

    private JsonObject reservationToJson(Reservation res) {
        JsonObject json = new JsonObject();
        json.addProperty("id", res.getId());
        json.addProperty("roomNumber", res.getRoomNumber());
        json.addProperty("guestName", res.getGuestName());
        json.addProperty("guestEmail", res.getGuestEmail());
        json.addProperty("guestCpf", res.getGuestCpf());
        json.addProperty("checkIn", res.getCheckIn().toString());
        json.addProperty("checkOut", res.getCheckOut().toString());
        json.addProperty("guests", res.getGuests());
        json.addProperty("ratePlan", res.getRatePlan().toString());
        json.addProperty("nights", res.getNights());
        json.addProperty("subtotal", res.getSubtotal().doubleValue());
        json.addProperty("discount", res.getDiscount().doubleValue());
        json.addProperty("total", res.getTotal().doubleValue());
        json.addProperty("status", res.getStatus().toString());
        json.addProperty("amountPaid", res.getAmountPaid().doubleValue());
        if (res.getRefund() != null) {
            json.addProperty("refund", res.getRefund().doubleValue());
        } else {
            json.add("refund", null);
        }
        return json;
    }

    private String getRoomType(int roomNumber) {
        if (roomNumber < 200) return "STANDARD";
        if (roomNumber < 300) return "DELUXE";
        return "SUITE";
    }

    private void validateRequiredFields(JsonObject body, String... fields) throws ApiException {
        for (String field : fields) {
            if (!body.has(field) || body.get(field).isJsonNull()) {
                throw new ApiException("INVALID_REQUEST", "Campo obrigatório: " + field, 400);
            }
        }
    }

    private void errorResponse(Context ctx, String errorCode, String message, int statusCode) {
        ctx.status(statusCode);
        JsonObject error = new JsonObject();
        error.addProperty("error", errorCode);
        error.addProperty("message", message);
        ctx.json(error);
    }
}
