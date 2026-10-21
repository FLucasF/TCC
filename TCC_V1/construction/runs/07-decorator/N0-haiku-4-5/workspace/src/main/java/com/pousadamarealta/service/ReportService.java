package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.util.DateUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReportService {
    private final ReservationService reservationService;

    public ReportService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationService.getReservations();

        List<Reservation> occupied = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                            r.getStatus() == ReservationStatus.CHECKED_IN ||
                            r.getStatus() == ReservationStatus.CHECKED_OUT)
                .filter(r -> !date.isBefore(r.getCheckIn()) && date.isBefore(r.getCheckOut()))
                .sorted((a, b) -> Integer.compare(a.getRoomNumber(), b.getRoomNumber()))
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : occupied) {
            csv.append(r.getRoomNumber()).append(",");
            csv.append(r.getRoomNumber() / 100 == 1 ? "STANDARD" : (r.getRoomNumber() / 100 == 2 ? "DELUXE" : "SUITE")).append(",");
            csv.append(r.getId()).append(",");
            csv.append(DateUtils.formatForApi(r.getCheckIn())).append(",");
            csv.append(DateUtils.formatForApi(r.getCheckOut())).append("\n");
        }

        csv.append("total,").append(occupied.size()).append("/5\n");

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationService.getReservations();

        List<Reservation> monthlyReservations = reservations.stream()
                .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
                .filter(r -> {
                    if (r.getStatus() == ReservationStatus.CANCELLED) {
                        return r.getAmountPaid() > 0 || (r.getRefund() != null && r.getRefund() > 0);
                    }
                    return r.getAmountPaid() > 0;
                })
                .sorted((a, b) -> {
                    int dateCompare = a.getCheckIn().compareTo(b.getCheckIn());
                    if (dateCompare == 0) {
                        return Long.compare(a.getId(), b.getId());
                    }
                    return dateCompare;
                })
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        double total = 0;

        for (Reservation r : monthlyReservations) {
            double amount = r.getAmountPaid();
            if (r.getRefund() != null) {
                amount -= r.getRefund();
            }

            csv.append(r.getId()).append(",");
            csv.append(r.getRoomNumber()).append(",");
            csv.append(DateUtils.formatForApi(r.getCheckIn())).append(",");
            csv.append(DateUtils.formatForApi(r.getCheckOut())).append(",");
            csv.append(r.getStatus()).append(",");
            csv.append(String.format("%.2f", amount)).append("\n");

            total += amount;
        }

        csv.append("total,").append(String.format("%.2f", total)).append("\n");

        return csv.toString();
    }
}
