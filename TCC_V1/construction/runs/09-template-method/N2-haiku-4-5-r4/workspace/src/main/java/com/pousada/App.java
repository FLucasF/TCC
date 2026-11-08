package com.pousada;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

public class App {
    public static void main(String[] args) {
        Database.init();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        var app = Javalin.create(config -> {
            config.jsonMapper(new GsonJsonMapper());
        }).start(port);

        app.get("/rooms", ctx -> {
            var rooms = RoomService.findAll();
            ctx.json(rooms);
        });

        app.post("/reservations", ctx -> {
            var body = ctx.bodyAsClass(CreateReservationRequest.class);
            var reservation = ReservationService.create(body);
            ctx.status(HttpStatus.CREATED);
            ctx.json(reservation);
        });

        app.get("/reservations/{id}", ctx -> {
            var id = Integer.parseInt(ctx.pathParam("id"));
            var reservation = ReservationService.findById(id);
            if (reservation == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                return;
            }
            ctx.json(reservation);
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            var id = Integer.parseInt(ctx.pathParam("id"));
            var body = ctx.bodyAsClass(ConfirmationRequest.class);
            var reservation = ReservationService.confirm(id, body.amountPaid);
            ctx.json(reservation);
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            var id = Integer.parseInt(ctx.pathParam("id"));
            var reservation = ReservationService.cancel(id);
            ctx.json(reservation);
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            var id = Integer.parseInt(ctx.pathParam("id"));
            var reservation = ReservationService.checkIn(id);
            ctx.json(reservation);
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            var id = Integer.parseInt(ctx.pathParam("id"));
            var document = ReservationService.checkOut(id);
            ctx.json(document);
        });

        app.get("/reservations/{id}/document", ctx -> {
            var id = Integer.parseInt(ctx.pathParam("id"));
            var document = ReservationService.getDocument(id);
            if (document == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reserva não encontrada"));
                return;
            }
            ctx.json(document);
        });

        app.get("/messages", ctx -> {
            var messages = MessageService.findAll();
            ctx.json(messages);
        });

        app.get("/reports/occupancy", ctx -> {
            var dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Data é obrigatória"));
                return;
            }
            var report = ReportService.generateOccupancy(dateStr);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        });

        app.get("/reports/revenue", ctx -> {
            var monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Mês é obrigatório"));
                return;
            }
            var report = ReportService.generateRevenue(monthStr);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        });

        app.get("/reports/tourism-tax", ctx -> {
            var monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Mês é obrigatório"));
                return;
            }
            var report = ReportService.generateTourismTax(monthStr);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        });

        app.get("/reports/rate-plans", ctx -> {
            var monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Mês é obrigatório"));
                return;
            }
            var report = ReportService.generateRatePlans(monthStr);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        });

        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.statusCodeNumber);
            ctx.json(new ErrorResponse(e.errorCode, e.message));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        });
    }
}
