package pousada.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import pousada.domain.Money;
import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.service.ReservationRepository;

public class RatePlanReport extends MonthlyReport {
    public RatePlanReport(ReservationRepository repository) {
        super(repository);
    }

    @Override
    public String name() {
        return "rate-plans";
    }

    @Override
    ReportData generate(YearMonth month) {
        List<Reservation> kept = keptMoneyStartingIn(month);
        List<List<String>> rows = Arrays.stream(RatePlan.values())
                .map(plan -> row(plan, kept.stream().filter(r -> r.ratePlan() == plan).toList()))
                .toList();
        BigDecimal total = kept.stream().map(Reservation::heldAmount).reduce(Money.ZERO, BigDecimal::add);
        return new ReportData(month.toString(), List.of("rate_plan", "reservations", "amount"), rows,
                List.of("total", String.valueOf(kept.size()), total.toPlainString()));
    }

    private static List<String> row(RatePlan plan, List<Reservation> reservations) {
        BigDecimal amount = reservations.stream().map(Reservation::heldAmount).reduce(Money.ZERO, BigDecimal::add);
        return List.of(plan.name(), String.valueOf(reservations.size()), amount.toPlainString());
    }
}
