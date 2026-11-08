package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.store.Tx;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class RatePlanReport extends MonthlyReport {
    @Override
    public String name() {
        return "rate-plans";
    }

    @Override
    protected ReportTable build(YearMonth month, Tx tx) {
        List<Reservation> reservations = retainedStartingIn(month, tx);
        List<List<String>> rows = new ArrayList<>();
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = reservations.stream().filter(r -> r.ratePlan() == plan).toList();
            rows.add(List.of(plan.name(), String.valueOf(ofPlan.size()), Money.plain(sum(ofPlan))));
        }
        return new ReportTable(name(), month.toString(), List.of("rate_plan", "reservations", "amount"), rows,
                List.of("total", String.valueOf(reservations.size()), Money.plain(sum(reservations))));
    }

    private static BigDecimal sum(List<Reservation> reservations) {
        return reservations.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
    }
}
