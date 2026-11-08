package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.store.Store;
import java.math.BigDecimal;
import java.util.List;

public class RevenueReport extends MonthlyReport {
    public RevenueReport(Store store) {
        super(store);
    }

    @Override public String name() { return "revenue"; }

    @Override
    protected ReportData build(String period, List<Reservation> startingInMonth) {
        List<Reservation> kept = retainingMoney(startingInMonth);
        List<List<String>> rows = kept.stream().map(r -> List.of(
                String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                r.checkOut().toString(), r.status().name(), Money.plain(r.retainedAmount()))).toList();
        BigDecimal sum = kept.stream().map(Reservation::retainedAmount).reduce(Money.ZERO, BigDecimal::add);
        return new ReportData(name(), period,
                List.of("reservation", "room", "check_in", "check_out", "status", "amount"), rows,
                List.of("total", Money.plain(sum)));
    }
}
