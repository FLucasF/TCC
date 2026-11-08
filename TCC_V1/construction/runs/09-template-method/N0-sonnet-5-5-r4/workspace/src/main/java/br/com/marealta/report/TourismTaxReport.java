package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.store.Tx;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public final class TourismTaxReport extends MonthlyReport {
    private final TourismTax tax;

    public TourismTaxReport(TourismTax tax) {
        this.tax = tax;
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    @Override
    protected ReportTable build(YearMonth month, Tx tx) {
        BigDecimal rate = tax.perGuestNight(month);
        List<Reservation> reservations = paidStartingIn(month, tx);
        List<List<String>> rows = reservations.stream().map(r -> List.of(
                String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                r.checkOut().toString(), String.valueOf(r.guests()), String.valueOf(r.nights()),
                Money.plain(taxOf(r, rate)))).toList();
        BigDecimal total = reservations.stream().map(r -> taxOf(r, rate)).reduce(Money.ZERO, BigDecimal::add);
        return new ReportTable(name(), month.toString(),
                List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"), rows,
                List.of("total", Money.plain(total)));
    }

    private static BigDecimal taxOf(Reservation r, BigDecimal rate) {
        return Money.round(rate.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }
}
