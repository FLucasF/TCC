package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.store.Store;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RatePlansReport extends MonthlyReport {
    public RatePlansReport(Store store) {
        super(store);
    }

    @Override public String name() { return "rate-plans"; }

    @Override
    protected ReportData build(String period, List<Reservation> startingInMonth) {
        List<Reservation> kept = retainingMoney(startingInMonth);
        List<List<String>> rows = new ArrayList<>();
        int count = 0;
        BigDecimal sum = Money.ZERO;
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = kept.stream().filter(r -> r.ratePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream().map(Reservation::retainedAmount).reduce(Money.ZERO, BigDecimal::add);
            rows.add(List.of(plan.name(), String.valueOf(ofPlan.size()), Money.plain(amount)));
            count += ofPlan.size();
            sum = sum.add(amount);
        }
        return new ReportData(name(), period, List.of("rate_plan", "reservations", "amount"), rows,
                List.of("total", String.valueOf(count), Money.plain(sum)));
    }
}
