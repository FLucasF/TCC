package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.store.Tx;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public final class RevenueReport extends MonthlyReport {
    @Override
    public String name() {
        return "revenue";
    }

    @Override
    protected ReportTable build(YearMonth month, Tx tx) {
        List<Reservation> reservations = retainedStartingIn(month, tx);
        List<List<String>> rows = reservations.stream().map(r -> List.of(
                String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                r.checkOut().toString(), r.status().name(), Money.plain(r.retained()))).toList();
        BigDecimal total = reservations.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
        return new ReportTable(name(), month.toString(),
                List.of("reservation", "room", "check_in", "check_out", "status", "amount"), rows,
                List.of("total", Money.plain(total)));
    }
}
