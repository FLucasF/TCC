package pousada.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import pousada.db.ReservationRepository;
import pousada.domain.Money;
import pousada.domain.RatePlan;
import pousada.domain.Reservation;

public class RatePlanReport extends MonthlyReport {
    public RatePlanReport(ReservationRepository reservations) {
        super(reservations);
    }

    @Override
    public String name() {
        return "rate-plans";
    }

    @Override
    protected Table table(YearMonth month) {
        List<Reservation> list = startingIn(month, MonthlyReport::keptMoney);
        List<String> rows = new ArrayList<>();
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = list.stream().filter(r -> r.getRatePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream().map(Reservation::retained).reduce(BigDecimal.ZERO, BigDecimal::add);
            rows.add(plan + "," + ofPlan.size() + "," + Money.plain(amount));
        }
        BigDecimal sum = list.stream().map(Reservation::retained).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Table("rate_plan,reservations,amount", rows, "total," + list.size() + "," + Money.plain(sum));
    }
}
