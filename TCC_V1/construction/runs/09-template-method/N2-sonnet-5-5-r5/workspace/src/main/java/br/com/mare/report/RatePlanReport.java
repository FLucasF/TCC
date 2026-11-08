package br.com.mare.report;

import br.com.mare.domain.Money;
import br.com.mare.domain.RatePlan;
import br.com.mare.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Uma linha por plano de tarifa, mesmo sem reservas, com as mesmas reservas do financeiro. */
public class RatePlanReport extends MonthlyReport {
    private final ReservationQueries queries;

    public RatePlanReport(ReservationQueries queries) {
        this.queries = queries;
    }

    @Override
    public String name() {
        return "rate-plans";
    }

    @Override
    protected String header() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected Body body(YearMonth month) {
        List<Reservation> list = queries.retainedStartingIn(month);
        List<String> rows = new ArrayList<>();
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = list.stream().filter(r -> r.ratePlan() == plan).toList();
            rows.add(plan + "," + ofPlan.size() + "," + Money.csv(sum(ofPlan)));
        }
        return new Body(rows, "total," + list.size() + "," + Money.csv(sum(list)));
    }

    private static BigDecimal sum(List<Reservation> list) {
        return list.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
    }
}
