package com.pousada.api;

import com.pousada.database.ReservationDao;
import com.pousada.database.RoomDao;
import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.Room;
import com.pousada.util.Dates;
import com.pousada.util.Money;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ReportHandler {
    public static void occupancy(Context ctx) throws Exception {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "date parameter required"));
                return;
            }

            LocalDate date = Dates.parseIso(dateStr);
            Map<Integer, Room> roomMap = RoomDao.findAllAsMap();

            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            int occupiedCount = 0;
            for (int roomNum : new int[]{101, 102, 201, 202, 301}) {
                List<Reservation> reservations = ReservationDao.findByRoomAndNight(roomNum, date);
                if (!reservations.isEmpty()) {
                    for (Reservation r : reservations) {
                        if (r.status != ReservationStatus.CANCELLED) {
                            Room room = roomMap.get(roomNum);
                            csv.append(roomNum).append(",")
                                    .append(room.type).append(",")
                                    .append(r.id).append(",")
                                    .append(Dates.formatCsv(r.checkIn)).append(",")
                                    .append(Dates.formatCsv(r.checkOut)).append("\n");
                            occupiedCount++;
                        }
                    }
                }
            }

            csv.append("total,").append(occupiedCount).append("/5\n");

            ctx.contentType("text/csv; charset=utf-8").result(csv.toString());
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        }
    }

    public static void revenue(Context ctx) throws Exception {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "month parameter required"));
                return;
            }

            YearMonth ym = YearMonth.parse(monthStr);
            List<Reservation> reservations = ReservationDao.findByMonthAndNotCancelled(ym.getYear(), ym.getMonthValue());

            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            BigDecimal total = BigDecimal.ZERO;

            for (Reservation r : reservations) {
                if (r.status != ReservationStatus.CANCELLED) {
                    BigDecimal amount = r.amountPaid;
                    if (r.refund != null) {
                        amount = amount.subtract(r.refund);
                    }

                    csv.append(r.id).append(",")
                            .append(r.roomNumber).append(",")
                            .append(Dates.formatCsv(r.checkIn)).append(",")
                            .append(Dates.formatCsv(r.checkOut)).append(",")
                            .append(r.status.name()).append(",")
                            .append(String.format("%.2f", amount)).append("\n");

                    total = total.add(amount);
                } else {
                    // Cancelled non-refundable reservations still count
                    if (r.amountPaid.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal amount = r.amountPaid;
                        if (r.refund != null) {
                            amount = amount.subtract(r.refund);
                        }

                        csv.append(r.id).append(",")
                                .append(r.roomNumber).append(",")
                                .append(Dates.formatCsv(r.checkIn)).append(",")
                                .append(Dates.formatCsv(r.checkOut)).append(",")
                                .append(r.status.name()).append(",")
                                .append(String.format("%.2f", amount)).append("\n");

                        total = total.add(amount);
                    }
                }
            }

            csv.append("total,,,,").append(String.format("%.2f", total)).append("\n");

            ctx.contentType("text/csv; charset=utf-8").result(csv.toString());
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        }
    }
}
