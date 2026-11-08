package pousada.report;

import pousada.Money;
import pousada.RatePlan;
import pousada.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Uma linha por plano de tarifa, mesmo sem reservas, com as mesmas reservas do relatório financeiro. */
public final class RatePlanReport extends MonthlyReport {
    private final ReportSource source;

    public RatePlanReport(ReportSource source) {
        super("rate-plans");
        this.source = source;
    }

    @Override
    protected ReportData build(YearMonth month) {
        List<Reservation> retained = source.retainedStartingIn(month);
        List<List<String>> rows = new ArrayList<>();
        int count = 0;
        BigDecimal total = Money.ZERO;
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = retained.stream().filter(r -> r.ratePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
            rows.add(List.of(plan.name(), String.valueOf(ofPlan.size()), Money.plain(amount)));
            count += ofPlan.size();
            total = total.add(amount);
        }
        return new ReportData(List.of("rate_plan", "reservations", "amount"), rows,
                List.of("total", String.valueOf(count), Money.plain(total)));
    }
}
