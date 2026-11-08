package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

/** How many reservations and how much money each rate plan brought in a month, for the management (CSV). */
public final class RatePlanReport extends CsvReport<YearMonth, RatePlanReport.PlanTotal> {

    /** One line: a rate plan with the reservations of the financial report that use it. */
    record PlanTotal(RatePlan plan, int reservations, BigDecimal amount) {}

    private final MonthlyReservations reservations;

    public RatePlanReport(ReservationStore store) {
        super("rate-plans");
        this.reservations = new MonthlyReservations(store);
    }

    @Override
    protected String header() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<PlanTotal> rows(YearMonth month) {
        List<Reservation> withRevenue = reservations.withRevenue(month);
        return Arrays.stream(RatePlan.values()).map(plan -> {
            List<Reservation> ofPlan = withRevenue.stream().filter(r -> r.ratePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream()
                    .map(Reservation::amountKept)
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
            return new PlanTotal(plan, ofPlan.size(), amount);
        }).toList();
    }

    @Override
    protected String line(PlanTotal row) {
        return row.plan() + "," + row.reservations() + "," + row.amount().toPlainString();
    }

    @Override
    protected String total(List<PlanTotal> rows) {
        int count = rows.stream().mapToInt(PlanTotal::reservations).sum();
        BigDecimal amount = rows.stream().map(PlanTotal::amount).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return "total," + count + "," + amount.toPlainString();
    }
}
