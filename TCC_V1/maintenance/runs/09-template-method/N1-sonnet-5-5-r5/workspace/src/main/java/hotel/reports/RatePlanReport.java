package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** How many reservations and how much money each rate plan brought in a month, for the management (CSV). */
public final class RatePlanReport extends CsvReport<YearMonth, RatePlanReport.PlanTotal> {

    /** One rate plan's line: the reservations counted by the financial report and the money kept. */
    record PlanTotal(RatePlan plan, int reservations, BigDecimal amount) {
    }

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
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
    protected List<PlanTotal> rows(YearMonth month) {
        List<Reservation> withRevenue = MonthlyRevenue.of(store, month);
        List<PlanTotal> totals = new ArrayList<>();
        for (RatePlan plan : RatePlan.values()) {
            int count = 0;
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : withRevenue) {
                if (reservation.ratePlan() == plan) {
                    count++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            totals.add(new PlanTotal(plan, count, amount));
        }
        return totals;
    }

    @Override
    protected String line(PlanTotal row) {
        return row.plan() + "," + row.reservations() + "," + row.amount().toPlainString();
    }

    @Override
    protected String total(List<PlanTotal> rows) {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
        for (PlanTotal row : rows) {
            count += row.reservations();
            amount = amount.add(row.amount());
        }
        return "total," + count + "," + amount.toPlainString();
    }
}
