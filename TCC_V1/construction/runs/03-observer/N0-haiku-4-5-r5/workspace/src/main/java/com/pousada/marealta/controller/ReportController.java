package com.pousada.marealta.controller;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;
import io.javalin.Javalin;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReportController {
    public static void register(Javalin app) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isEmpty()) {
                    ctx.status(400);
                    ctx.json(error("INVALID_REQUEST", "Date parameter is required"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr);
                String csv = generateOccupancyReport(date);

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (DateTimeParseException e) {
                ctx.status(400);
                ctx.json(error("INVALID_REQUEST", "Invalid date format"));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(error("DATABASE_ERROR", e.getMessage()));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400);
                    ctx.json(error("INVALID_REQUEST", "Month parameter is required"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthStr);
                String csv = generateRevenueReport(month);

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (DateTimeParseException e) {
                ctx.status(400);
                ctx.json(error("INVALID_REQUEST", "Invalid month format"));
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(error("DATABASE_ERROR", e.getMessage()));
            }
        });
    }

    private static String generateOccupancyReport(LocalDate date) throws SQLException {
        var roomRepository = new RoomRepository();
        var reservationRepository = new ReservationRepository();
        var rooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;
        for (var room : rooms) {
            var reservations = reservationRepository.findByRoomAndDate(room.getNumber(), date);
            if (!reservations.isEmpty()) {
                for (var res : reservations) {
                    csv.append(room.getNumber()).append(",")
                        .append(room.getType().name()).append(",")
                        .append(res.getId()).append(",")
                        .append(res.getCheckIn()).append(",")
                        .append(res.getCheckOut()).append("\n");
                    occupiedCount++;
                }
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(rooms.size()).append("\n");

        return csv.toString();
    }

    private static String generateRevenueReport(YearMonth month) throws SQLException {
        ReservationRepository reservationRepository = new ReservationRepository();
        var reservations = reservationRepository.findByMonth(month.getYear(), month.getMonthValue());

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
        for (var res : reservations) {
            java.math.BigDecimal refund = res.getRefund() != null ? res.getRefund() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal amount = res.getAmountPaid().subtract(refund);

            if (amount.signum() != 0) {
                csv.append(res.getId()).append(",")
                    .append(res.getRoomNumber()).append(",")
                    .append(res.getCheckIn()).append(",")
                    .append(res.getCheckOut()).append(",")
                    .append(res.getStatus().name()).append(",")
                    .append(formatMoney(amount)).append("\n");

                totalAmount = totalAmount.add(amount);
            }
        }

        csv.append("total,").append(formatMoney(totalAmount)).append("\n");

        return csv.toString();
    }

    private static String formatMoney(java.math.BigDecimal value) {
        return String.format("%.2f", value);
    }

    private static Object error(String code, String message) {
        String errorCode = code;
        String errorMessage = message;
        return new Object() {
            public final String error = errorCode;
            public final String message = errorMessage;
        };
    }
}
