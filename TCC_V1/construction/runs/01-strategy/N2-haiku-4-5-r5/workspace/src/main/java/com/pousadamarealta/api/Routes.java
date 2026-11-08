package com.pousadamarealta.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pousadamarealta.database.MessageDao;
import com.pousadamarealta.database.Message;
import com.pousadamarealta.database.RoomDao;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.api.request.CreateReservationRequest;
import com.pousadamarealta.api.response.*;

import java.time.LocalDate;
import java.util.*;
import java.sql.SQLException;

public class Routes {
    private static final Gson gson = new GsonBuilder()
        .serializeNulls()
        .create();

    public static void setup(Javalin app) {
        app.get("/rooms", ctx -> handleGetRooms(ctx));
        app.post("/reservations", ctx -> handleCreateReservation(ctx));
        app.get("/reservations/{id}", ctx -> handleGetReservation(ctx));
        app.post("/reservations/{id}/confirmation", ctx -> handleConfirmReservation(ctx));
        app.post("/reservations/{id}/cancellation", ctx -> handleCancelReservation(ctx));
        app.post("/reservations/{id}/check-in", ctx -> handleCheckIn(ctx));
        app.post("/reservations/{id}/check-out", ctx -> handleCheckOut(ctx));
        app.get("/reservations/{id}/document", ctx -> handleGetDocument(ctx));
        app.get("/messages", ctx -> handleGetMessages(ctx));
        app.get("/reports/occupancy", ctx -> handleGetOccupancyReport(ctx));
        app.get("/reports/revenue", ctx -> handleGetRevenueReport(ctx));

        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getErrorMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        });
    }

    private static void handleGetRooms(Context ctx) throws SQLException {
        List<Room> rooms = RoomDao.getAllRooms();
        List<RoomResponse> responses = new ArrayList<>();

        for (Room room : rooms) {
            responses.add(new RoomResponse(
                room.number(),
                room.type().toString(),
                room.capacity(),
                room.nightlyRate()
            ));
        }

        ctx.json(responses);
    }

    private static void handleCreateReservation(Context ctx) {
        try {
            CreateReservationRequest request = gson.fromJson(ctx.body(), CreateReservationRequest.class);
            Reservation res = ReservationService.createReservation(request);
            ctx.status(201);
            ctx.json(buildReservationResponse(res));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid request body");
        }
    }

    private static void handleGetReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation res = ReservationService.getReservation(id);
        ctx.json(buildReservationResponse(res));
    }

    private static void handleConfirmReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation res = ReservationService.confirmReservation(id);
        ctx.json(buildReservationResponse(res));
    }

    private static void handleCancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation res = ReservationService.cancelReservation(id);
        ctx.json(buildReservationResponse(res));
    }

    private static void handleCheckIn(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation res = ReservationService.checkIn(id);
        ctx.json(buildReservationResponse(res));
    }

    private static void handleCheckOut(Context ctx) throws SQLException {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation res = ReservationService.checkOut(id);

        Room room = RoomDao.getRoomByNumber(res.getRoomNumber());
        VoucherResponse voucher = new VoucherResponse(
            res.getId(),
            res.getGuestName(),
            res.getGuestCpf(),
            res.getRoomNumber(),
            room.type().toString(),
            res.getCheckIn(),
            res.getCheckOut(),
            res.getNights(),
            res.getSubtotal(),
            res.getDiscount(),
            res.getTotal()
        );

        ctx.json(voucher);
    }

    private static void handleGetDocument(Context ctx) throws SQLException {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation res = ReservationService.getReservation(id);

        if (res.getAmountPaid().compareTo(java.math.BigDecimal.ZERO) == 0) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Document is only available for paid reservations");
        }

        Room room = RoomDao.getRoomByNumber(res.getRoomNumber());
        VoucherResponse voucher = new VoucherResponse(
            res.getId(),
            res.getGuestName(),
            res.getGuestCpf(),
            res.getRoomNumber(),
            room.type().toString(),
            res.getCheckIn(),
            res.getCheckOut(),
            res.getNights(),
            res.getSubtotal(),
            res.getDiscount(),
            res.getTotal()
        );

        ctx.json(voucher);
    }

    private static void handleGetMessages(Context ctx) throws SQLException {
        List<Message> messages = MessageDao.getAllMessages();
        List<MessageResponse> responses = new ArrayList<>();

        for (Message msg : messages) {
            responses.add(new MessageResponse(msg.id(), msg.channel(), msg.to(), msg.subject(), msg.body()));
        }

        ctx.json(responses);
    }

    private static void handleGetOccupancyReport(Context ctx) {
        String date = ctx.queryParam("date");
        if (date == null) {
            throw new ApiException(400, "INVALID_REQUEST", "date parameter is required");
        }

        String report = ReportService.getOccupancyReport(date);
        ctx.header("Content-Type", "text/csv; charset=utf-8");
        ctx.result(report);
    }

    private static void handleGetRevenueReport(Context ctx) {
        String month = ctx.queryParam("month");
        if (month == null) {
            throw new ApiException(400, "INVALID_REQUEST", "month parameter is required");
        }

        String report = ReportService.getRevenueReport(month);
        ctx.header("Content-Type", "text/csv; charset=utf-8");
        ctx.result(report);
    }

    private static ReservationResponse buildReservationResponse(Reservation res) {
        return new ReservationResponse(
            res.getId(),
            res.getRoomNumber(),
            res.getGuestName(),
            res.getGuestEmail(),
            res.getGuestCpf(),
            res.getCheckIn(),
            res.getCheckOut(),
            res.getGuests(),
            res.getRatePlan().toString(),
            res.getNights(),
            res.getSubtotal(),
            res.getDiscount(),
            res.getTotal(),
            res.getStatus().toString(),
            res.getAmountPaid(),
            res.getRefund()
        );
    }
}
