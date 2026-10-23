package com.marealta.domain.service;

import com.marealta.domain.model.Reservation;
import com.marealta.domain.model.ReservationStatus;
import com.marealta.domain.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String generateOccupancyReport(LocalDate date, List<Reservation> reservations, List<Room> allRooms) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = reservations.stream()
            .filter(r -> r.getStatus() != ReservationStatus.PENDING && r.getStatus() != ReservationStatus.CANCELLED)
            .filter(r -> !r.getCheckIn().isAfter(date) && r.getCheckOut().isAfter(date))
            .sorted((r1, r2) -> r1.getRoomNumber().compareTo(r2.getRoomNumber()))
            .toList();

        for (Reservation reservation : occupiedReservations) {
            Room room = allRooms.stream()
                .filter(r -> r.number().equals(reservation.getRoomNumber()))
                .findFirst()
                .orElseThrow();

            csv.append(room.number()).append(",")
               .append(room.type()).append(",")
               .append(reservation.getId()).append(",")
               .append(reservation.getCheckIn().format(DATE_FORMAT)).append(",")
               .append(reservation.getCheckOut().format(DATE_FORMAT)).append("\n");
        }

        csv.append("total,").append(occupiedReservations.size())
           .append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month, List<Reservation> reservations) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> relevantReservations = reservations.stream()
            .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
            .filter(r -> {
                BigDecimal revenue = r.getAmountPaid();
                if (r.getRefund() != null) {
                    revenue = revenue.subtract(r.getRefund());
                }
                return revenue.compareTo(BigDecimal.ZERO) > 0;
            })
            .sorted((r1, r2) -> {
                int dateCompare = r1.getCheckIn().compareTo(r2.getCheckIn());
                if (dateCompare != 0) return dateCompare;
                return r1.getId().compareTo(r2.getId());
            })
            .toList();

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : relevantReservations) {
            BigDecimal amount = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                amount = amount.subtract(reservation.getRefund());
            }
            total = total.add(amount);

            csv.append(reservation.getId()).append(",")
               .append(reservation.getRoomNumber()).append(",")
               .append(reservation.getCheckIn().format(DATE_FORMAT)).append(",")
               .append(reservation.getCheckOut().format(DATE_FORMAT)).append(",")
               .append(reservation.getStatus()).append(",")
               .append(formatAmount(amount)).append("\n");
        }

        csv.append("total,").append(formatAmount(total));

        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}
