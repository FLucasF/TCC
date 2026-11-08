package com.pousada.report;

import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RevenueReport extends Report {
    private final YearMonth month;
    private final List<Reservation> reservations;
    private final DecimalFormat currencyFormatter = new DecimalFormat("0.00");

    public RevenueReport(YearMonth month) {
        this.month = month;
        this.reservations = ReservationRepository.findByMonthAndStatuses(
                month.getYear(), month.getMonthValue(),
                "CONFIRMED", "CHECKED_IN", "CHECKED_OUT"
        );
    }

    @Override
    protected String buildTitle() {
        return "Pousada Maré Alta,revenue," + month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    @Override
    protected String buildHeader() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> buildDataLines() {
        List<String> lines = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Reservation res : reservations) {
            BigDecimal amount = res.amountPaid;
            if (res.refund != null) {
                amount = amount.subtract(res.refund);
            }

            lines.add(res.id + "," + res.roomNumber + "," +
                    res.checkIn.format(formatter) + "," +
                    res.checkOut.format(formatter) + "," +
                    res.status.name() + "," +
                    currencyFormatter.format(amount));
        }

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : reservations) {
            BigDecimal amount = res.amountPaid;
            if (res.refund != null) {
                amount = amount.subtract(res.refund);
            }
            total = total.add(amount);
        }

        return "total," + currencyFormatter.format(total);
    }
}
