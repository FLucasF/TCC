package pousada.report;

import pousada.domain.Money;
import pousada.domain.Reservation;

import java.math.BigDecimal;
import java.util.List;

public class RevenueReport implements Report {
    @Override
    public String name() {
        return "revenue";
    }

    @Override
    public PeriodKind periodKind() {
        return PeriodKind.MONTH;
    }

    @Override
    public ReportTable build(Period period, ReportSource source) {
        List<Reservation> reservations = source.retainedStartingIn(period);
        BigDecimal sum = reservations.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
        List<List<String>> rows = reservations.stream().map(r -> List.of(
                String.valueOf(r.id()),
                String.valueOf(r.roomNumber()),
                r.checkIn().toString(),
                r.checkOut().toString(),
                r.status().name(),
                r.retained().toPlainString())).toList();
        return new ReportTable(
                List.of("reservation", "room", "check_in", "check_out", "status", "amount"),
                rows,
                List.of("total", sum.toPlainString()));
    }
}
