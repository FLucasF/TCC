package br.com.maretalta.report;

import br.com.maretalta.domain.Money;
import br.com.maretalta.domain.RatePlan;
import br.com.maretalta.domain.Reservation;
import br.com.maretalta.store.Store;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RatePlanReport extends MonthlyReport {
    @Override public String name() { return "rate-plans"; }

    @Override
    protected Report build(Store.Tx tx, YearMonth month) {
        List<Reservation> reservations = retainedMoney(tx, month);
        List<List<String>> rows = new ArrayList<>();
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = reservations.stream().filter(r -> r.ratePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
            rows.add(List.of(plan.name(), String.valueOf(ofPlan.size()), Money.plain(amount)));
        }
        BigDecimal total = reservations.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
        return new Report(name(), month.toString(), List.of("rate_plan", "reservations", "amount"), rows,
                List.of("total", String.valueOf(reservations.size()), Money.plain(total)));
    }
}
