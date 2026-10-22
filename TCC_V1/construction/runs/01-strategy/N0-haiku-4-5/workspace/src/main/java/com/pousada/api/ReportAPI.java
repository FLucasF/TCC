package com.pousada.api;

import com.pousada.db.Reservation;
import com.pousada.db.Room;
import com.pousada.service.ReservationService;
import com.pousada.service.RoomService;
import com.pousada.util.DateFormatter;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportAPI {
    public static void register(Javalin app) {
        app.get("/reports/occupancy", ctx -> getOccupancyReport(ctx));
        app.get("/reports/revenue", ctx -> getRevenueReport(ctx));
    }

    private static void getOccupancyReport(io.javalin.http.Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null || dateParam.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
                return;
            }

            LocalDate date;
            try {
                date = DateFormatter.parseISO(dateParam);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            List<Room> allRooms = RoomService.getAllRooms();
            List<Reservation> reservations = ReservationService.getReservationsByDate(date);

            Map<Integer, Reservation> reservationsByRoom = reservations.stream()
                .collect(Collectors.toMap(r -> r.roomNumber, r -> r));

            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            int occupiedCount = 0;
            for (Room room : allRooms) {
                if (reservationsByRoom.containsKey(room.number)) {
                    Reservation res = reservationsByRoom.get(room.number);
                    csv.append(res.roomNumber).append(",")
                       .append(room.type).append(",")
                       .append(res.id).append(",")
                       .append(DateFormatter.formatISO(res.checkIn)).append(",")
                       .append(DateFormatter.formatISO(res.checkOut)).append("\n");
                    occupiedCount++;
                }
            }

            csv.append("total,").append(occupiedCount).append("/").append(allRooms.size()).append("\n");

            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(csv.toString());
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
        }
    }

    private static void getRevenueReport(io.javalin.http.Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
                return;
            }

            LocalDate monthStart;
            try {
                YearMonth ym = YearMonth.parse(monthParam);
                monthStart = ym.atDay(1);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
                return;
            }

            List<Reservation> reservations = ReservationService.getReservationsByMonth(monthStart);

            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Reservation res : reservations) {
                BigDecimal amount = res.amountPaid;
                if (res.refund != null && res.refund.compareTo(BigDecimal.ZERO) > 0) {
                    amount = amount.subtract(res.refund);
                }

                csv.append(res.id).append(",")
                   .append(res.roomNumber).append(",")
                   .append(DateFormatter.formatISO(res.checkIn)).append(",")
                   .append(DateFormatter.formatISO(res.checkOut)).append(",")
                   .append(res.status).append(",")
                   .append(String.format("%.2f", amount)).append("\n");

                totalAmount = totalAmount.add(amount);
            }

            csv.append("total,,,,").append(String.format("%.2f", totalAmount)).append("\n");

            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(csv.toString());
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
        }
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
