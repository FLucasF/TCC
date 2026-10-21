package com.pousada.marealta.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.ReservationStatus;
import com.pousada.marealta.util.FormatHelper;

public class RevenueReportService {
    private final ReservationService reservationService;

    public RevenueReportService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public String generateRevenueReport(String monthStr) {
        YearMonth month = YearMonth.parse(monthStr);
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> relevantReservations = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservationService.getAllReservations()) {
            YearMonth resMonth = YearMonth.from(res.checkIn);
            if (resMonth.equals(month)) {
                BigDecimal amount = res.amountPaid;
                if (res.refund != null) {
                    amount = amount.subtract(res.refund);
                }

                if (amount.signum() >= 0) {
                    relevantReservations.add(res);
                    total = total.add(amount);
                }
            }
        }

        relevantReservations.sort((a, b) -> {
            int dateCompare = a.checkIn.compareTo(b.checkIn);
            if (dateCompare != 0) return dateCompare;
            return Long.compare(a.id, b.id);
        });

        for (Reservation res : relevantReservations) {
            BigDecimal amount = res.amountPaid;
            if (res.refund != null) {
                amount = amount.subtract(res.refund);
            }

            csv.append(res.id).append(",")
                .append(res.roomNumber).append(",")
                .append(FormatHelper.formatIsoDate(res.checkIn)).append(",")
                .append(FormatHelper.formatIsoDate(res.checkOut)).append(",")
                .append(res.status).append(",")
                .append(FormatHelper.formatCurrencyCsv(amount)).append("\n");
        }

        csv.append("total,,,,").append(FormatHelper.formatCurrencyCsv(total)).append("\n");

        return csv.toString();
    }
}
