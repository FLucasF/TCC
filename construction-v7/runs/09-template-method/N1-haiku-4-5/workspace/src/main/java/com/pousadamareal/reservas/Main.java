package com.pousadamareal.reservas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamareal.reservas.dto.*;
import com.pousadamareal.reservas.service.ApiException;
import com.pousadamareal.reservas.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        ReservationService service = new ReservationService();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JsonMapper() {
                @Override
                public <T> T fromJsonString(String json, Type targetType) {
                    try {
                        return mapper.readValue(json, mapper.constructType(targetType));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public String toJsonString(Object obj, Type type) {
                    try {
                        return mapper.writeValueAsString(obj);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }).start(port);

        app.exception(ApiException.class, (e, ctx) -> {
            ErrorResponse error = new ErrorResponse(e.getErrorCode(), e.getMessage());
            ctx.status(e.getStatus());
            ctx.json(error);
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ErrorResponse error = new ErrorResponse("INVALID_REQUEST", e.getMessage());
            ctx.status(400);
            ctx.json(error);
        });

        app.get("/rooms", ctx -> {
            ctx.json(service.getAllRooms());
        });

        app.post("/reservations", ctx -> {
            CreateReservationRequest request = ctx.bodyAsClass(CreateReservationRequest.class);
            validateCreateReservationRequest(request);
            ReservationResponse response = service.createReservation(request);
            ctx.status(201);
            ctx.json(response);
        });

        app.get("/reservations/{id}", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            ReservationResponse response = service.getReservation(id);
            ctx.json(response);
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            String bodyStr = ctx.body();

            if (bodyStr == null || bodyStr.isEmpty()) {
                throw new ApiException(400, "INVALID_REQUEST", "Request body is required");
            }

            try {
                ConfirmationRequest request = mapper.readValue(bodyStr, ConfirmationRequest.class);
                ReservationResponse response = service.confirmReservation(id, request.amountPaid);
                ctx.json(response);
            } catch (Exception e) {
                throw new ApiException(400, "INVALID_REQUEST", "Invalid request body");
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            ReservationResponse response = service.cancelReservation(id);
            ctx.json(response);
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            ReservationResponse response = service.checkIn(id);
            ctx.json(response);
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            DocumentResponse response = service.checkOut(id);
            ctx.json(response);
        });

        app.get("/reservations/{id}/document", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            DocumentResponse response = service.getDocument(id);
            ctx.json(response);
        });

        app.get("/messages", ctx -> {
            ctx.json(service.getMessages());
        });

        app.get("/reports/occupancy", ctx -> {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                throw new ApiException(400, "INVALID_REQUEST", "date parameter is required");
            }

            try {
                LocalDate date = LocalDate.parse(dateStr);
                String report = service.generateOccupancyReport(date);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                throw new ApiException(400, "INVALID_REQUEST", "Invalid date format");
            }
        });

        app.get("/reports/revenue", ctx -> {
            String month = ctx.queryParam("month");
            if (month == null || month.isEmpty()) {
                throw new ApiException(400, "INVALID_REQUEST", "month parameter is required");
            }

            try {
                validateMonthFormat(month);
                String report = service.generateRevenueReport(month);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                throw new ApiException(400, "INVALID_REQUEST", "Invalid month format");
            }
        });

        app.get("/reports/tourism-tax", ctx -> {
            String month = ctx.queryParam("month");
            if (month == null || month.isEmpty()) {
                throw new ApiException(400, "INVALID_REQUEST", "month parameter is required");
            }

            try {
                validateMonthFormat(month);
                String report = service.generateTourismTaxReport(month);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                throw new ApiException(400, "INVALID_REQUEST", "Invalid month format");
            }
        });

        app.get("/reports/rate-plans", ctx -> {
            String month = ctx.queryParam("month");
            if (month == null || month.isEmpty()) {
                throw new ApiException(400, "INVALID_REQUEST", "month parameter is required");
            }

            try {
                validateMonthFormat(month);
                String report = service.generateRatePlansReport(month);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                throw new ApiException(400, "INVALID_REQUEST", "Invalid month format");
            }
        });
    }

    private static void validateCreateReservationRequest(CreateReservationRequest request) {
        if (request.roomNumber == 0 || request.guestName == null || request.guestEmail == null ||
            request.guestCpf == null || request.checkIn == null || request.checkOut == null ||
            request.guests == 0 || request.ratePlan == null) {
            throw new ApiException(400, "INVALID_REQUEST", "Missing required fields");
        }
    }

    private static void validateMonthFormat(String month) {
        if (!month.matches("\\d{4}-\\d{2}")) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid month format");
        }
        try {
            LocalDate.parse(month + "-01");
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid month format");
        }
    }

    static class ConfirmationRequest {
        public BigDecimal amountPaid;
    }
}
