package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.api.dto.*;
import com.pousada.models.*;
import com.pousada.services.*;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JsonMapper;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Main {
    private static final ReservationService reservationService = new ReservationService();
    private static final MessageService messageService = new MessageService();
    private static final ReportService reportService = new ReportService(reservationService);
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        int portNumber = port != null ? Integer.parseInt(port) : 7070;

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(createJsonMapper());
        }).start(portNumber);

        setupRoutes(app);

        System.out.println("Pousada Maré Alta listening on port " + portNumber);
    }

    private static void setupRoutes(Javalin app) {
        app.get("/rooms", ctx -> {
            List<Room> rooms = reservationService.getAllRooms();
            List<RoomResponse> responses = rooms.stream()
                .map(room -> new RoomResponse(room.getNumber(), room.getType().toString(),
                                             room.getCapacity(), room.getNightlyRate()))
                .toList();
            ctx.json(responses);
        });

        app.post("/reservations", ctx -> {
            CreateReservationRequest req = ctx.bodyAsClass(CreateReservationRequest.class);
            validateCreateReservationRequest(req);

            try {
                Reservation reservation = reservationService.createReservation(
                    req.roomNumber, req.guestName, req.guestEmail, req.guestCpf,
                    req.checkIn, req.checkOut, req.guests, req.ratePlan, req.companyCode
                );
                ctx.status(HttpStatus.CREATED);
                ctx.json(toReservationResponse(reservation));
            } catch (ReservationService.ApiException e) {
                ctx.status(e.getStatusCode());
                ctx.json(new ErrorResponse(e.getCode(), e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                ctx.json(toReservationResponse(reservation));
            } catch (ReservationService.ApiException e) {
                ctx.status(e.getStatusCode());
                ctx.json(new ErrorResponse(e.getCode(), e.getMessage()));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                String body = ctx.body();
                BigDecimal amountPaid = BigDecimal.ZERO;
                if (body != null && !body.isEmpty()) {
                    var map = ctx.bodyAsClass(java.util.Map.class);
                    if (map.containsKey("amountPaid")) {
                        amountPaid = BigDecimal.valueOf(((Number) map.get("amountPaid")).doubleValue());
                    }
                } else {
                    amountPaid = reservation.getTotal();
                }

                Reservation confirmed = reservationService.confirmReservation(id, amountPaid);

                Room room = reservationService.getRoom(confirmed.getRoomNumber());
                String emailBody = ReceiptFormatter.formatConfirmationEmail(confirmed, room, confirmed.getGuestName());
                messageService.sendMessage("EMAIL", confirmed.getGuestEmail(),
                                          "Reserva confirmada - Pousada Maré Alta", emailBody);

                ctx.json(toReservationResponse(confirmed));
            } catch (ReservationService.ApiException e) {
                ctx.status(e.getStatusCode());
                ctx.json(new ErrorResponse(e.getCode(), e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation checkedIn = reservationService.checkIn(id);
                ctx.json(toReservationResponse(checkedIn));
            } catch (ReservationService.ApiException e) {
                ctx.status(e.getStatusCode());
                ctx.json(new ErrorResponse(e.getCode(), e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation checkedOut = reservationService.checkOut(id);
                Room room = reservationService.getRoom(checkedOut.getRoomNumber());
                String receipt = ReceiptFormatter.formatReceipt(checkedOut, room);
                ctx.result(receipt);
                ctx.header("Content-Type", "text/plain; charset=utf-8");
            } catch (ReservationService.ApiException e) {
                ctx.status(e.getStatusCode());
                ctx.json(new ErrorResponse(e.getCode(), e.getMessage()));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);

                if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED &&
                    reservation.getStatus() != Reservation.ReservationStatus.CHECKED_IN &&
                    reservation.getStatus() != Reservation.ReservationStatus.CHECKED_OUT) {
                    throw new ReservationService.ApiException("OPERATION_NOT_ALLOWED",
                        "Segunda via disponível apenas para reservas pagas", 409);
                }

                Room room = reservationService.getRoom(reservation.getRoomNumber());
                String receipt = ReceiptFormatter.formatReceipt(reservation, room);
                ctx.result(receipt);
                ctx.header("Content-Type", "text/plain; charset=utf-8");
            } catch (ReservationService.ApiException e) {
                ctx.status(e.getStatusCode());
                ctx.json(new ErrorResponse(e.getCode(), e.getMessage()));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation cancelled = reservationService.cancelReservation(id);
                ctx.json(toReservationResponse(cancelled));
            } catch (ReservationService.ApiException e) {
                ctx.status(e.getStatusCode());
                ctx.json(new ErrorResponse(e.getCode(), e.getMessage()));
            }
        });

        app.get("/messages", ctx -> {
            List<Message> messages = messageService.getAllMessages();
            List<MessageResponse> responses = messages.stream()
                .map(m -> new MessageResponse(m.getId(), m.getChannel(), m.getTo(), m.getSubject(), m.getBody()))
                .toList();
            ctx.json(responses);
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String date = ctx.queryParam("date");
                if (date == null) {
                    throw new ReservationService.ApiException("INVALID_REQUEST", "Parâmetro 'date' obrigatório", 400);
                }
                String report = reportService.getOccupancyReport(date);
                ctx.result(report);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
            } catch (ReservationService.ApiException e) {
                ctx.status(e.getStatusCode());
                ctx.json(new ErrorResponse(e.getCode(), e.getMessage()));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String month = ctx.queryParam("month");
                if (month == null) {
                    throw new ReservationService.ApiException("INVALID_REQUEST", "Parâmetro 'month' obrigatório", 400);
                }
                String report = reportService.getRevenueReport(month);
                ctx.result(report);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
            } catch (ReservationService.ApiException e) {
                ctx.status(e.getStatusCode());
                ctx.json(new ErrorResponse(e.getCode(), e.getMessage()));
            }
        });

        app.exception(Exception.class, (e, ctx) -> {
            System.err.println("Erro não tratado: " + e.getMessage());
            e.printStackTrace();
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        });
    }

    private static void validateCreateReservationRequest(CreateReservationRequest req) {
        if (req.roomNumber <= 0 || req.guestName == null || req.guestName.isEmpty() ||
            req.guestEmail == null || req.guestEmail.isEmpty() ||
            req.guestCpf == null || req.guestCpf.isEmpty() ||
            req.checkIn == null || req.checkOut == null ||
            req.guests <= 0 || req.ratePlan == null) {
            throw new ReservationService.ApiException("INVALID_REQUEST", "Campos obrigatórios faltando", 400);
        }
    }

    private static ReservationResponse toReservationResponse(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.id = reservation.getId();
        response.roomNumber = reservation.getRoomNumber();
        response.guestName = reservation.getGuestName();
        response.guestEmail = reservation.getGuestEmail();
        response.guestCpf = reservation.getGuestCpf();
        response.checkIn = reservation.getCheckIn().format(dateFormatter);
        response.checkOut = reservation.getCheckOut().format(dateFormatter);
        response.guests = reservation.getGuests();
        response.ratePlan = reservation.getRatePlan().getCode();
        response.nights = reservation.getNights();
        response.subtotal = reservation.getSubtotal();
        response.discount = reservation.getDiscount();
        response.total = reservation.getTotal();
        response.status = reservation.getStatus().toString();
        response.amountPaid = reservation.getAmountPaid();
        response.refund = reservation.getRefund();
        return response;
    }

    private static JsonMapper createJsonMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new JsonMapper() {
            @Override
            public String toJsonString(Object obj, Type type) {
                try {
                    return objectMapper.writeValueAsString(obj);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T fromJsonString(String json, Type targetType) {
                try {
                    return objectMapper.readValue(json, objectMapper.getTypeFactory().constructType(targetType));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T fromJsonStream(InputStream stream, Type targetType) {
                try {
                    return objectMapper.readValue(stream, objectMapper.getTypeFactory().constructType(targetType));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
