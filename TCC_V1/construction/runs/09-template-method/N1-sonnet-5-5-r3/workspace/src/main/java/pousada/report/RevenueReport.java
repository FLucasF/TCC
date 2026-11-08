package pousada.report;

import pousada.Money;
import pousada.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public final class RevenueReport extends MonthlyReport {
    private final ReportSource source;

    public RevenueReport(ReportSource source) {
        super("revenue");
        this.source = source;
    }

    @Override
    protected ReportData build(YearMonth month) {
        List<Reservation> retained = source.retainedStartingIn(month);
        List<List<String>> rows = retained.stream().map(r -> List.of(
                String.valueOf(r.id()),
                String.valueOf(r.roomNumber()),
                r.checkIn().toString(),
                r.checkOut().toString(),
                r.status().name(),
                Money.plain(r.retained()))).toList();
        BigDecimal total = retained.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
        return new ReportData(List.of("reservation", "room", "check_in", "check_out", "status", "amount"), rows,
                List.of("total", Money.plain(total)));
    }
}
