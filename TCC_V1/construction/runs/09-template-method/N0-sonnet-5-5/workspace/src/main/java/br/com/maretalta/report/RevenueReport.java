package br.com.maretalta.report;

import br.com.maretalta.domain.Money;
import br.com.maretalta.domain.Reservation;
import br.com.maretalta.store.Store;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport extends MonthlyReport {
    @Override public String name() { return "revenue"; }

    @Override
    protected Report build(Store.Tx tx, YearMonth month) {
        List<Reservation> reservations = retainedMoney(tx, month);
        List<List<String>> rows = reservations.stream().map(r -> List.of(
                String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                r.checkOut().toString(), r.status().name(), Money.plain(r.retained()))).toList();
        BigDecimal total = reservations.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
        return new Report(name(), month.toString(),
                List.of("reservation", "room", "check_in", "check_out", "status", "amount"), rows,
                List.of("total", Money.plain(total)));
    }
}
