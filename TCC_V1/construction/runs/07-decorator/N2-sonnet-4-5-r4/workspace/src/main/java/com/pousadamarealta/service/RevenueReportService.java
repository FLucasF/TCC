package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RevenueReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService;

    public RevenueReportService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public String generateReport(YearMonth month) {
        List<Reservation> reservations = reservationService.getReservationsByMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal netAmount = calculateNetAmount(reservation);

            csv.append(String.format("%d,%s,%s,%s,%s,%.2f\n",
                reservation.getId(),
                reservation.getRoomNumber(),
                reservation.getCheckIn().format(DATE_FORMAT),
                reservation.getCheckOut().format(DATE_FORMAT),
                reservation.getStatus().name(),
                netAmount
            ));

            total = total.add(netAmount);
        }

        csv.append(String.format("total,%.2f", total));

        return csv.toString();
    }

    private BigDecimal calculateNetAmount(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refund = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refund);
    }
}
