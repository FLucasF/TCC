package pousada.report;

import pousada.domain.Money;
import pousada.domain.RatePlan;
import pousada.domain.Reservation;

import java.math.BigDecimal;
import java.util.List;

public class RatePlanReport implements Report {
    @Override
    public String name() {
        return "rate-plans";
    }

    @Override
    public PeriodKind periodKind() {
        return PeriodKind.MONTH;
    }

    @Override
    public ReportTable build(Period period, ReportSource source) {
        List<Reservation> reservations = source.retainedStartingIn(period);
        List<List<String>> rows = List.of(RatePlan.values()).stream().map(plan -> {
            List<Reservation> ofPlan = reservations.stream().filter(r -> r.ratePlan() == plan).toList();
            return List.of(plan.name(), String.valueOf(ofPlan.size()), amountOf(ofPlan).toPlainString());
        }).toList();
        return new ReportTable(
                List.of("rate_plan", "reservations", "amount"),
                rows,
                List.of("total", String.valueOf(reservations.size()), amountOf(reservations).toPlainString()));
    }

    private static BigDecimal amountOf(List<Reservation> reservations) {
        return reservations.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
    }
}
