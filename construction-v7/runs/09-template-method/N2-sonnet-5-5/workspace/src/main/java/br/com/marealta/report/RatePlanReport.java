package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RatePlanReport extends MonthlyCsvReport {
    @Override public String name() { return "rate-plans"; }

    @Override
    protected Table build(YearMonth month, List<Reservation> all) {
        List<Reservation> included = retainingMoney(month, all);
        List<List<String>> rows = new ArrayList<>();
        int count = 0;
        BigDecimal sum = Money.ZERO;
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = included.stream().filter(r -> r.ratePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
            rows.add(List.of(plan.name(), String.valueOf(ofPlan.size()), Money.plain(amount)));
            count += ofPlan.size();
            sum = sum.add(amount);
        }
        return new Table(List.of("rate_plan", "reservations", "amount"), rows,
                List.of("total", String.valueOf(count), Money.plain(sum)));
    }
}
