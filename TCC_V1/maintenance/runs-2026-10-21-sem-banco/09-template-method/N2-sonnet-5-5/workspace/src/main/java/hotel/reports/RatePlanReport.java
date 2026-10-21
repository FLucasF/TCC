package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

/** How many reservations, and how much money kept, each rate plan brought in a month (CSV). */
public final class RatePlanReport extends Report<YearMonth, RatePlanReport.PlanSummary> {

    /** One line of the report. */
    record PlanSummary(RatePlan plan, int reservations, BigDecimal amount) {
    }

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    @Override
    protected String name() {
        return "rate-plans";
    }

    @Override
    protected String header() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<PlanSummary> rows(YearMonth month) {
        List<Reservation> withRevenue = MonthlyReservations.withRevenue(store, month);
        return Arrays.stream(RatePlan.values()).map(plan -> summarize(plan, withRevenue)).toList();
    }

    private static PlanSummary summarize(RatePlan plan, List<Reservation> withRevenue) {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : withRevenue) {
            if (reservation.ratePlan() == plan) {
                count++;
                amount = amount.add(reservation.amountKept());
            }
        }
        return new PlanSummary(plan, count, amount);
    }

    @Override
    protected String line(PlanSummary summary) {
        return summary.plan() + "," + summary.reservations() + "," + summary.amount().toPlainString();
    }

    @Override
    protected String total(List<PlanSummary> summaries) {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
        for (PlanSummary summary : summaries) {
            count += summary.reservations();
            amount = amount.add(summary.amount());
        }
        return "total," + count + "," + amount.toPlainString();
    }
}
