package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport extends MonthlyCsvReport {
    @Override public String name() { return "revenue"; }

    @Override
    protected Table build(YearMonth month, List<Reservation> all) {
        List<Reservation> included = retainingMoney(month, all);
        List<List<String>> rows = included.stream().map(r -> List.of(
                String.valueOf(r.id()), String.valueOf(r.room().number()),
                r.checkInDate().toString(), r.checkOutDate().toString(),
                r.status().name(), Money.plain(r.retained()))).toList();
        BigDecimal sum = included.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
        return new Table(List.of("reservation", "room", "check_in", "check_out", "status", "amount"), rows,
                List.of("total", Money.plain(sum)));
    }
}
