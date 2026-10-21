package br.com.marealta.reports;

import br.com.marealta.Money;
import br.com.marealta.RatePlan;
import br.com.marealta.Reservation;
import br.com.marealta.ReservationService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

public class RatePlanReport extends MonthlyReport<RatePlanReport.Line> {
    public record Line(RatePlan plan, long count, BigDecimal amount) {}

    public RatePlanReport(ReservationService reservations) {
        super(reservations);
    }

    @Override
    public String name() { return "rate-plans"; }

    @Override
    protected List<String> columns() {
        return List.of("rate_plan", "reservations", "amount");
    }

    @Override
    protected List<Line> select(YearMonth month) {
        List<Reservation> counted = retainingMoney(month);
        return Arrays.stream(RatePlan.values()).map(plan -> {
            List<Reservation> ofPlan = counted.stream().filter(r -> r.getRatePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
            return new Line(plan, ofPlan.size(), amount);
        }).toList();
    }

    @Override
    protected List<String> cells(Line line) {
        return List.of(line.plan().name(), String.valueOf(line.count()), Money.csv(line.amount()));
    }

    @Override
    protected List<String> total(YearMonth month, List<Line> lines) {
        long count = lines.stream().mapToLong(Line::count).sum();
        BigDecimal amount = lines.stream().map(Line::amount).reduce(Money.ZERO, BigDecimal::add);
        return List.of("total", String.valueOf(count), Money.csv(amount));
    }
}
