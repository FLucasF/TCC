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
    protected ReportContent generate(YearMonth month) {
        List<Reservation> kept = withMoneyKept(month);
        List<List<String>> rows = new ArrayList<>();
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = kept.stream().filter(r -> r.ratePlan() == plan).toList();
            rows.add(List.of(plan.name(), String.valueOf(ofPlan.size()), Money.csv(sum(ofPlan))));
        }
        return new ReportContent(month.toString(),
                List.of("rate_plan", "reservations", "amount"),
                rows, List.of("total", String.valueOf(kept.size()), Money.csv(sum(kept))));
    }

    private static BigDecimal sum(List<Reservation> reservations) {
        return reservations.stream().map(Reservation::kept).reduce(Money.ZERO, BigDecimal::add);
    }
}
