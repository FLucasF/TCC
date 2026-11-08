package pousada.reports;

import pousada.domain.Money;
import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.repo.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RatePlanReport extends MonthReport {
    public RatePlanReport(ReservationRepository reservations) {
        super(reservations);
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
        List<Reservation> list = withRetainedMoney(month);
        List<String> rows = new ArrayList<>();
        int count = 0;
        BigDecimal total = Money.ZERO;
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = list.stream().filter(r -> r.ratePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
            rows.add(plan.name() + "," + ofPlan.size() + "," + Money.plain(amount));
            count += ofPlan.size();
            total = total.add(amount);
        }
        return new Body(rows, "total," + count + "," + Money.plain(total));
    }
}
