package com.pousada.services;

import com.pousada.db.Database;
import com.pousada.models.Room;
import com.pousada.models.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

public class ReportService {
    private final Database db;

    public ReportService(Database db) {
        this.db = db;
    }

    public String getOccupancyReport(LocalDate date) {
        Map<Integer, Reservation> occupancy = db.getReservationsForNight(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Room room : Room.FIXED_ROOMS) {
            Reservation reservation = occupancy.get(room.getNumber());
            if (reservation != null) {
                csv.append(room.getNumber()).append(",")
                   .append(room.getType()).append(",")
                   .append(reservation.getId()).append(",")
                   .append(formatDate(reservation.getCheckIn())).append(",")
                   .append(formatDate(reservation.getCheckOut())).append("\n");
            }
        }

        int occupiedRooms = occupancy.size();
        csv.append("total,").append(occupiedRooms).append("/5\n");

        return csv.toString();
    }

    public String getRevenueReport(String monthString) {
        YearMonth yearMonth = YearMonth.parse(monthString);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth().plusDays(1);

        java.util.List<Reservation> reservations = db.getReservationsForMonth(monthStart, monthEnd);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal revenue = res.getAmountPaid();
            if (res.getRefund() != null) {
                revenue = revenue.subtract(res.getRefund());
            }

            csv.append(res.getId()).append(",")
               .append(res.getRoomNumber()).append(",")
               .append(formatDate(res.getCheckIn())).append(",")
               .append(formatDate(res.getCheckOut())).append(",")
               .append(res.getStatus()).append(",")
               .append(formatMoney(revenue)).append("\n");

            total = total.add(revenue);
        }

        csv.append("total,,,,").append(formatMoney(total)).append("\n");

        return csv.toString();
    }

    private String formatDate(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private String formatMoney(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toString();
    }
}
