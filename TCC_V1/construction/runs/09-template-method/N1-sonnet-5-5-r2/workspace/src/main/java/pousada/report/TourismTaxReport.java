package pousada.report;

import pousada.domain.Money;
import pousada.domain.Reservation;

import java.math.BigDecimal;
import java.util.List;

public class TourismTaxReport implements Report {
    private final BigDecimal ratePerGuestNight;

    public TourismTaxReport(BigDecimal ratePerGuestNight) {
        this.ratePerGuestNight = ratePerGuestNight;
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    @Override
    public PeriodKind periodKind() {
        return PeriodKind.MONTH;
    }

    @Override
    public ReportTable build(Period period, ReportSource source) {
        List<Reservation> reservations = source.paidStartingIn(period);
        BigDecimal sum = reservations.stream().map(this::taxOf).reduce(Money.ZERO, BigDecimal::add);
        List<List<String>> rows = reservations.stream().map(r -> List.of(
                String.valueOf(r.id()),
                String.valueOf(r.roomNumber()),
                r.checkIn().toString(),
                r.checkOut().toString(),
                String.valueOf(r.guests()),
                String.valueOf(r.nights()),
                taxOf(r).toPlainString())).toList();
        return new ReportTable(
                List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"),
                rows,
                List.of("total", sum.toPlainString()));
    }

    private BigDecimal taxOf(Reservation r) {
        return Money.round(ratePerGuestNight.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }
}
