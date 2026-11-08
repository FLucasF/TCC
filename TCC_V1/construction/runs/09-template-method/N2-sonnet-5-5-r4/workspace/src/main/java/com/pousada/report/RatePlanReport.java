package com.pousada.report;

import com.pousada.domain.Money;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RatePlanReport extends MonthlyReport {
    public RatePlanReport(ReservationRepository reservations) {
        super(reservations);
    }

    @Override
    public String name() {
        return "rate-plans";
    }

    @Override
    protected List<String> header() {
        return List.of("rate_plan", "reservations", "amount");
    }

    @Override
    protected Body body(YearMonth month) {
        List<Reservation> kept = retainingMoney(month);
        List<List<String>> rows = new ArrayList<>();
        int count = 0;
        BigDecimal total = Money.ZERO;
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = kept.stream().filter(r -> r.getRatePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream().map(Reservation::retainedAmount).reduce(Money.ZERO, BigDecimal::add);
            rows.add(List.of(plan.name(), String.valueOf(ofPlan.size()), Money.plain(amount)));
            count += ofPlan.size();
            total = total.add(amount);
        }
        return new Body(rows, List.of("total", String.valueOf(count), Money.plain(total)));
    }
}
