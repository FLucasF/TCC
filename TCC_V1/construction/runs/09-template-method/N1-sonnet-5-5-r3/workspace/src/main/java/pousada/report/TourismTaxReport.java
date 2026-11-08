package pousada.report;

import pousada.Money;
import pousada.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public final class TourismTaxReport extends MonthlyReport {
    private final ReportSource source;
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReportSource source, BigDecimal taxPerGuestNight) {
        super("tourism-tax");
        this.source = source;
        this.taxPerGuestNight = taxPerGuestNight;
    }

    @Override
    protected ReportData build(YearMonth month) {
        List<Reservation> paid = source.paidStartingIn(month);
        List<List<String>> rows = paid.stream().map(r -> List.of(
                String.valueOf(r.id()),
                String.valueOf(r.roomNumber()),
                r.checkIn().toString(),
                r.checkOut().toString(),
                String.valueOf(r.guests()),
                String.valueOf(r.nights()),
                Money.plain(taxOf(r)))).toList();
        BigDecimal total = paid.stream().map(this::taxOf).reduce(Money.ZERO, BigDecimal::add);
        return new ReportData(List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"), rows,
                List.of("total", Money.plain(total)));
    }

    private BigDecimal taxOf(Reservation r) {
        return Money.round(taxPerGuestNight.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }
}
