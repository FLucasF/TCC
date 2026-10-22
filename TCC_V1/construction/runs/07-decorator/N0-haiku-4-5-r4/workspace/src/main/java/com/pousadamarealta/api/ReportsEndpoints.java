package com.pousadamarealta.api;

import io.javalin.Javalin;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportsEndpoints {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    static class ErrorResponse {
        String error;
        String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }

    public static void register(Javalin app) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateParam = ctx.queryParam("date");
                if (dateParam == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "date parameter required"));
                    return;
                }

                LocalDate date;
                try {
                    date = LocalDate.parse(dateParam, DATE_FORMAT);
                } catch (Exception e) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                    return;
                }

                List<Reservation> reservations = ReservationRepository.findByNightForOccupancy(date);
                List<Room> allRooms = RoomRepository.findAll();

                StringBuilder csv = new StringBuilder();
                csv.append("room,type,reservation,check_in,check_out\n");

                for (Room room : allRooms) {
                    for (Reservation res : reservations) {
                        if (res.roomNumber == room.number) {
                            csv.append(room.number).append(",")
                               .append(room.type).append(",")
                               .append(res.id).append(",")
                               .append(res.checkIn).append(",")
                               .append(res.checkOut).append("\n");
                            break;
                        }
                    }
                }

                long occupiedCount = reservations.stream()
                        .map(r -> r.roomNumber)
                        .distinct()
                        .count();

                csv.append("total,").append(occupiedCount).append("/").append(allRooms.size()).append("\n");

                ctx.contentType("text/csv; charset=utf-8").result(csv.toString());
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "month parameter required"));
                    return;
                }

                int year, month;
                try {
                    YearMonth ym = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
                    year = ym.getYear();
                    month = ym.getMonthValue();
                } catch (Exception e) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
                    return;
                }

                List<Reservation> reservations = ReservationRepository.findByMonthForRevenue(year, month);

                StringBuilder csv = new StringBuilder();
                csv.append("reservation,room,check_in,check_out,status,amount\n");

                java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;

                for (Reservation res : reservations) {
                    java.math.BigDecimal amount = res.amountPaid;
                    if (res.refund != null) {
                        amount = res.amountPaid.subtract(res.refund);
                    }
                    totalAmount = totalAmount.add(amount);

                    csv.append(res.id).append(",")
                       .append(res.roomNumber).append(",")
                       .append(res.checkIn).append(",")
                       .append(res.checkOut).append(",")
                       .append(res.status).append(",")
                       .append(String.format("%.2f", amount)).append("\n");
                }

                csv.append("total,").append(String.format("%.2f", totalAmount)).append("\n");

                ctx.contentType("text/csv; charset=utf-8").result(csv.toString());
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });
    }
}
