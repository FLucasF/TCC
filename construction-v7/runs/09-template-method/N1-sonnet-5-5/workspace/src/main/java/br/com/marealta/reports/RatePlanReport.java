package br.com.marealta.reports;

import br.com.marealta.Money;
import br.com.marealta.RatePlan;
import br.com.marealta.Reservation;
import br.com.marealta.ReservationService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RatePlanReport extends MonthReport {

    public RatePlanReport(ReservationService reservations) {
        super("rate-plans", reservations);
    }

    @Override
    Table table(YearMonth month) {
        List<Reservation> kept = retainingMoneyIn(month);
        List<List<String>> rows = new ArrayList<>();
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = kept.stream().filter(r -> r.ratePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream().map(Reservation::retained)
                    .reduce(Money.round(BigDecimal.ZERO), BigDecimal::add);
            rows.add(List.of(plan.name(), String.valueOf(ofPlan.size()), Money.csv(amount)));
        }
        BigDecimal total = kept.stream().map(Reservation::retained)
                .reduce(Money.round(BigDecimal.ZERO), BigDecimal::add);
        return new Table(List.of("rate_plan", "reservations", "amount"), rows,
                List.of("total", String.valueOf(kept.size()), Money.csv(total)));
    }
}
