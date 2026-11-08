package com.marealta.report;

import com.marealta.Money;
import com.marealta.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public final class TourismTaxReport extends Report {
    /** Valor por hóspede por noite; a prefeitura muda todo ano. */
    static final BigDecimal TAX_PER_GUEST_NIGHT = Money.of("4.50");

    private final YearMonth month;
    private final List<Reservation> reservations;

    public TourismTaxReport(ReportData data, YearMonth month) {
        this.month = month;
        this.reservations = data.paidIn(month);
    }

    @Override
    protected String name() { return "tourism-tax"; }

    @Override
    protected String period() { return month.toString(); }

    @Override
    protected List<String> header() {
        return List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax");
    }

    @Override
    protected List<List<String>> rows() {
        return reservations.stream().map(r -> List.of(
                String.valueOf(r.id()),
                String.valueOf(r.roomNumber()),
                r.checkIn().toString(),
                r.checkOut().toString(),
                String.valueOf(r.guests()),
                String.valueOf(r.nights()),
                Money.plain(tax(r)))).toList();
    }

    @Override
    protected List<String> total() {
        BigDecimal sum = reservations.stream().map(TourismTaxReport::tax).reduce(Money.ZERO, BigDecimal::add);
        return List.of("total", Money.plain(sum));
    }

    private static BigDecimal tax(Reservation r) {
        return Money.round(TAX_PER_GUEST_NIGHT.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }
}
