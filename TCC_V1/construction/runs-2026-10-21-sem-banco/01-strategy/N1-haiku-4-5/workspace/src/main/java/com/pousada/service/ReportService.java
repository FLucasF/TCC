package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.domain.ReservationStatus;
import com.pousada.util.Money;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ReservationService reservationService;

    public ReportService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;

        for (Room room : reservationService.getAllRooms()) {
            List<Reservation> reservations = reservationService.getReservationsForRoom(room.getNumber(), date);
            if (!reservations.isEmpty()) {
                Reservation res = reservations.get(0);
                csv.append(room.getNumber()).append(",");
                csv.append(room.getType().getName()).append(",");
                csv.append(res.getId()).append(",");
                csv.append(res.getCheckIn().format(DATE_FORMAT)).append(",");
                csv.append(res.getCheckOut().format(DATE_FORMAT)).append("\n");
                occupiedCount++;
            }
        }

        csv.append("total,").append(occupiedCount).append("/5\n");
        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        double total = 0.0;

        List<Reservation> reservations = reservationService.getReservationsStartingInMonth(month);
        for (Reservation res : reservations) {
            double amount = calculateAmount(res);
            if (amount > 0) {
                csv.append(res.getId()).append(",");
                csv.append(res.getRoomNumber()).append(",");
                csv.append(res.getCheckIn().format(DATE_FORMAT)).append(",");
                csv.append(res.getCheckOut().format(DATE_FORMAT)).append(",");
                csv.append(res.getStatus().toString()).append(",");
                csv.append(Money.formatForCsv(amount)).append("\n");
                total += amount;
            }
        }

        csv.append("total,,,,").append(Money.formatForCsv(total)).append("\n");
        return csv.toString();
    }

    private double calculateAmount(Reservation res) {
        double amount = res.getAmountPaid();
        if (res.getRefund() != null) {
            amount -= res.getRefund();
        }
        return amount;
    }
}
