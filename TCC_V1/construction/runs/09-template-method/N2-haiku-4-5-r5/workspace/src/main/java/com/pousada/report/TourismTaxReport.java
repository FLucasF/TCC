package com.pousada.report;

import com.pousada.domain.Money;
import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TourismTaxReport extends Report {
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");
    private final YearMonth month;
    private final List<Reservation> reservations;
    private final DecimalFormat currencyFormatter = new DecimalFormat("0.00");

    public TourismTaxReport(YearMonth month) {
        this.month = month;
        this.reservations = ReservationRepository.findByMonthAndStatuses(
                month.getYear(), month.getMonthValue(),
                "CONFIRMED", "CHECKED_IN", "CHECKED_OUT"
        );
    }

    @Override
    protected String buildTitle() {
        return "Pousada Maré Alta,tourism-tax," + month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    @Override
    protected String buildHeader() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> buildDataLines() {
        List<String> lines = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Reservation res : reservations) {
            BigDecimal tax = Money.multiply(
                    TAX_PER_GUEST_PER_NIGHT,
                    new BigDecimal(res.guests * res.nights)
            );

            lines.add(res.id + "," + res.roomNumber + "," +
                    res.checkIn.format(formatter) + "," +
                    res.checkOut.format(formatter) + "," +
                    res.guests + "," +
                    res.nights + "," +
                    currencyFormatter.format(tax));
        }

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : reservations) {
            BigDecimal tax = Money.multiply(
                    TAX_PER_GUEST_PER_NIGHT,
                    new BigDecimal(res.guests * res.nights)
            );
            total = total.add(tax);
        }

        return "total," + currencyFormatter.format(total);
    }
}
