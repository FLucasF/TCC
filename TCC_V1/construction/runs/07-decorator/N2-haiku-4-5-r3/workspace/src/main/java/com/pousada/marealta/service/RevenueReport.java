package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.infrastructure.ReservationRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport {
    private final ReservationRepository reservationRepository;

    public RevenueReport(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public String generate(YearMonth yearMonth) throws Exception {
        List<Reservation> reservations = reservationRepository.findByMonth(
            yearMonth.getYear(), yearMonth.getMonthValue());

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid().subtract(
                res.getRefund() != null ? res.getRefund() : BigDecimal.ZERO);
            total = total.add(amount);

            csv.append(res.getId()).append(",")
               .append(res.getRoomNumber()).append(",")
               .append(res.getCheckIn()).append(",")
               .append(res.getCheckOut()).append(",")
               .append(res.getStatus()).append(",")
               .append(formatCurrency(amount)).append("\n");
        }

        csv.append("total,,,,").append(formatCurrency(total)).append("\n");

        return csv.toString();
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format(java.util.Locale.US, "%.2f", amount);
    }
}
