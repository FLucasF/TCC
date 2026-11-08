package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.store.Tx;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport implements Report {
    @Override
    public String name() { return "revenue"; }

    @Override
    public String param() { return "month"; }

    @Override
    public ReportTable build(Tx tx, String rawPeriod) {
        YearMonth month = Periods.month(rawPeriod);
        List<Reservation> kept = Ledger.keptStartingIn(tx, month);
        BigDecimal sum = Money.ZERO;
        for (Reservation r : kept) {
            sum = sum.add(r.kept());
        }
        List<List<String>> rows = kept.stream()
                .map(r -> List.of(String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                        r.checkOut().toString(), r.status().name(), Money.plain(r.kept())))
                .toList();
        return new ReportTable(name(), month.toString(),
                List.of("reservation", "room", "check_in", "check_out", "status", "amount"), rows,
                List.of("total", Money.plain(sum)));
    }
}
