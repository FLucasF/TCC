package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** How many reservations and how much money each rate plan brought in a month, for the management (CSV). */
public final class RatePlanReport extends CsvReport<YearMonth> {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "rate-plans";
    }

    @Override
    protected Table table(YearMonth month) {
        List<Reservation> withRevenue = MonthlyReservations.withRevenueStartingIn(store, month);
        List<String> rows = new ArrayList<>();
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = withRevenue.stream().filter(r -> r.ratePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream().map(Reservation::amountKept).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
            rows.add(plan + "," + ofPlan.size() + "," + amount.toPlainString());
            totalCount += ofPlan.size();
            totalAmount = totalAmount.add(amount);
        }
        return new Table("rate_plan,reservations,amount", rows, "total," + totalCount + "," + totalAmount.toPlainString());
    }
}
