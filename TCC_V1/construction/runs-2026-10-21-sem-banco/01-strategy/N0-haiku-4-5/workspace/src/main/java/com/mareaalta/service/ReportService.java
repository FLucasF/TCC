package com.mareaalta.service;

import com.mareaalta.model.Reservation;
import com.mareaalta.model.Room;
import com.mareaalta.data.RoomData;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter CSV_MONEY_FORMAT = DateTimeFormatter.ofPattern("0.00");

    public String generateOccupancyReport(String dateStr, Collection<Reservation> reservations) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMAT);
            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            List<Reservation> occupiedRooms = new ArrayList<>();
            for (Reservation res : reservations) {
                if (res.status.equals("CANCELLED")) continue;
                if (!isConfirmedOrLater(res)) continue;

                LocalDate checkIn = LocalDate.parse(res.checkIn, DATE_FORMAT);
                LocalDate checkOut = LocalDate.parse(res.checkOut, DATE_FORMAT);

                if (!date.isBefore(checkIn) && date.isBefore(checkOut)) {
                    occupiedRooms.add(res);
                }
            }

            occupiedRooms.sort(Comparator.comparingInt(r -> r.roomNumber));

            int totalOccupied = 0;
            for (Reservation res : occupiedRooms) {
                Room room = RoomData.getRooms().stream()
                    .filter(r -> r.number() == res.roomNumber)
                    .findFirst()
                    .orElse(null);
                if (room != null) {
                    csv.append(res.roomNumber).append(",")
                        .append(room.type()).append(",")
                        .append(res.id).append(",")
                        .append(res.checkIn).append(",")
                        .append(res.checkOut).append("\n");
                    totalOccupied++;
                }
            }

            csv.append("total,").append(totalOccupied).append("/5\n");
            return csv.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isConfirmedOrLater(Reservation res) {
        return res.status.equals("CONFIRMED") || res.status.equals("CHECKED_IN") || res.status.equals("CHECKED_OUT");
    }

    public String generateRevenueReport(String monthStr, Collection<Reservation> reservations) {
        try {
            YearMonth month = YearMonth.parse(monthStr);
            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            List<Reservation> monthReservations = new ArrayList<>();
            for (Reservation res : reservations) {
                LocalDate checkIn = LocalDate.parse(res.checkIn, DATE_FORMAT);
                if (YearMonth.from(checkIn).equals(month)) {
                    double amount = calculateAmountForRevenue(res);
                    if (amount > 0) {
                        monthReservations.add(res);
                    }
                }
            }

            monthReservations.sort((r1, r2) -> {
                LocalDate d1 = LocalDate.parse(r1.checkIn, DATE_FORMAT);
                LocalDate d2 = LocalDate.parse(r2.checkIn, DATE_FORMAT);
                int dateCompare = d1.compareTo(d2);
                if (dateCompare != 0) return dateCompare;
                return Integer.compare(r1.id, r2.id);
            });

            double totalAmount = 0;
            for (Reservation res : monthReservations) {
                double amount = calculateAmountForRevenue(res);
                csv.append(res.id).append(",")
                    .append(res.roomNumber).append(",")
                    .append(res.checkIn).append(",")
                    .append(res.checkOut).append(",")
                    .append(res.status).append(",")
                    .append(formatMoney(amount)).append("\n");
                totalAmount += amount;
            }

            csv.append("total,,,,").append(formatMoney(totalAmount)).append("\n");
            return csv.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private double calculateAmountForRevenue(Reservation res) {
        double paid = res.amountPaid;
        double refunded = res.refund != null ? res.refund : 0;
        return paid - refunded;
    }

    private String formatMoney(double value) {
        return String.format("%.2f", value).replace(",", ".");
    }
}
