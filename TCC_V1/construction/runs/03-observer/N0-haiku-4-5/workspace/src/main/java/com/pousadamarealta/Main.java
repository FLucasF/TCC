package com.pousadamarealta;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import com.pousadamarealta.exceptions.*;
import com.pousadamarealta.api.*;

public class Main {
    public static void main(String[] args) {
        Database.initialize();
        var app = Javalin.create();

        var port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        app.get("/rooms", ctx -> {
            var rooms = ReservationService.getRooms();
            ctx.json(rooms);
        });

        app.post("/reservations", ctx -> {
            try {
                var json = ctx.bodyAsClass(ReservationRequest.class);
                var reservation = ReservationService.createReservation(json);
                ctx.status(HttpStatus.CREATED);
                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (IllegalArgumentException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            } catch (NotFoundException e) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            } catch (ConflictException e) {
                ctx.status(HttpStatus.CONFLICT);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            } catch (UnprocessableException e) {
                ctx.status(422);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            }
        });

        app.get("/reservations/:id", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = ReservationService.getReservation(id);
                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (NotFoundException e) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            }
        });

        app.post("/reservations/:id/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = ReservationService.confirmReservation(id);
                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (NotFoundException e) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            } catch (ConflictException e) {
                ctx.status(HttpStatus.CONFLICT);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            }
        });

        app.post("/reservations/:id/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = ReservationService.cancelReservation(id);
                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (NotFoundException e) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            } catch (ConflictException e) {
                ctx.status(HttpStatus.CONFLICT);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            }
        });

        app.post("/reservations/:id/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var reservation = ReservationService.checkIn(id);
                ctx.json(ReservationResponse.fromReservation(reservation));
            } catch (NotFoundException e) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            } catch (ConflictException e) {
                ctx.status(HttpStatus.CONFLICT);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            }
        });

        app.post("/reservations/:id/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var document = ReservationService.checkOut(id);
                ctx.json(document);
            } catch (NotFoundException e) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            } catch (ConflictException e) {
                ctx.status(HttpStatus.CONFLICT);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            }
        });

        app.get("/reservations/:id/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                var document = ReservationService.getDocument(id);
                ctx.json(document);
            } catch (NotFoundException e) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new ErrorResponse(e.code, e.getMessage()));
            }
        });

        app.get("/messages", ctx -> {
            var messages = MessageService.getAllMessages();
            ctx.json(messages);
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                var csv = ReportService.generateOccupancyReport(dateStr);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (IllegalArgumentException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                var csv = ReportService.generateRevenueReport(monthStr);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (IllegalArgumentException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
            }
        });

        app.start(port);
    }
}
