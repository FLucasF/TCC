package com.pousadamarealta.services;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.utils.Formatting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    private final Database database;

    public ReportService(Database database) {
        this.database = database;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = database.getReservationsForOccupancyReport(date);
        List<com.pousadamarealta.domain.Room> allRooms = database.getAllRooms();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (var room : allRooms) {
            var reservation = reservations.stream()
                .filter(r -> r.getRoomNumber() == room.getNumber())
                .findFirst();

            if (reservation.isPresent()) {
                var res = reservation.get();
                csv.append(res.getRoomNumber()).append(",")
                   .append(res.getRoomNumber() == 301 ? "SUITE" : (res.getRoomNumber() < 200 ? "STANDARD" : "DELUXE")).append(",")
                   .append(res.getId()).append(",")
                   .append(Formatting.formatDateIso(res.getCheckIn())).append(",")
                   .append(Formatting.formatDateIso(res.getCheckOut())).append("\n");
            }
        }

        int occupiedRooms = (int) reservations.stream()
            .map(Reservation::getRoomNumber)
            .distinct()
            .count();

        csv.append("total,").append(occupiedRooms).append("/").append(allRooms.size()).append("\n");

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth yearMonth) {
        List<Reservation> reservations = database.getReservationsForRevenueReport(
            yearMonth.getYear(),
            yearMonth.getMonthValue()
        );

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (var res : reservations) {
            BigDecimal amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount = amount.subtract(res.getRefund());
            }

            csv.append(res.getId()).append(",")
               .append(res.getRoomNumber()).append(",")
               .append(Formatting.formatDateIso(res.getCheckIn())).append(",")
               .append(Formatting.formatDateIso(res.getCheckOut())).append(",")
               .append(res.getStatus().getValue()).append(",")
               .append(Formatting.formatMoneyForCsv(amount)).append("\n");

            total = total.add(amount);
        }

        csv.append("total,,,,").append(Formatting.formatMoneyForCsv(total)).append("\n");

        return csv.toString();
    }
}
