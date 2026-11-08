package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.store.Tx;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RatePlanReport implements Report {
    @Override
    public String name() { return "rate-plans"; }

    @Override
    public String param() { return "month"; }

    @Override
    public ReportTable build(Tx tx, String rawPeriod) {
        YearMonth month = Periods.month(rawPeriod);
        List<Reservation> kept = Ledger.keptStartingIn(tx, month);
        List<List<String>> rows = new ArrayList<>();
        int totalCount = 0;
        BigDecimal totalAmount = Money.ZERO;
        for (RatePlan plan : RatePlan.values()) {
            int count = 0;
            BigDecimal amount = Money.ZERO;
            for (Reservation r : kept) {
                if (r.ratePlan() == plan) {
                    count++;
                    amount = amount.add(r.kept());
                }
            }
            rows.add(List.of(plan.name(), String.valueOf(count), Money.plain(amount)));
            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }
        return new ReportTable(name(), month.toString(), List.of("rate_plan", "reservations", "amount"), rows,
                List.of("total", String.valueOf(totalCount), Money.plain(totalAmount)));
    }
}
