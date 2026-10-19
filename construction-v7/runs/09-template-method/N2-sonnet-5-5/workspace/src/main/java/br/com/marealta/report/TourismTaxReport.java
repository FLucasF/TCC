package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class TourismTaxReport extends MonthlyCsvReport {
    private final TourismTaxPolicy policy;

    public TourismTaxReport(TourismTaxPolicy policy) { this.policy = policy; }

    @Override public String name() { return "tourism-tax"; }

    @Override
    protected Table build(YearMonth month, List<Reservation> all) {
        List<Reservation> paid = startingIn(month, all).stream().filter(Reservation::isPaid).toList();
        List<List<String>> rows = paid.stream().map(r -> List.of(
                String.valueOf(r.id()), String.valueOf(r.room().number()),
                r.checkInDate().toString(), r.checkOutDate().toString(),
                String.valueOf(r.guests()), String.valueOf(r.nights()),
                Money.plain(policy.taxFor(r)))).toList();
        BigDecimal sum = paid.stream().map(policy::taxFor).reduce(Money.ZERO, BigDecimal::add);
        return new Table(List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"), rows,
                List.of("total", Money.plain(sum)));
    }
}
