package br.com.marealta.report;

import br.com.marealta.db.ReservationRepository;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Reservations and money kept per rate plan, over the same reservations as the revenue report. */
public final class RatePlanReport implements Report {

    private final ReservationRepository reservations;

    public RatePlanReport(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override public String name() { return "rate-plans"; }

    @Override public PeriodKind periodKind() { return PeriodKind.MONTH; }

    @Override
    public ReportTable build(Connection c, String period) throws SQLException {
        List<Reservation> list = MonthlyReservations.withRevenue(reservations, c, period);
        List<List<String>> rows = new ArrayList<>();
        BigDecimal total = Money.ZERO;
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = list.stream().filter(r -> r.ratePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream().map(Reservation::amountKept).reduce(Money.ZERO, BigDecimal::add);
            total = total.add(amount);
            rows.add(List.of(plan.name(), String.valueOf(ofPlan.size()), ReportRenderer.money(amount)));
        }
        return new ReportTable(name(), period, List.of("rate_plan", "reservations", "amount"), rows,
                List.of("total", String.valueOf(list.size()), ReportRenderer.money(total)));
    }
}
