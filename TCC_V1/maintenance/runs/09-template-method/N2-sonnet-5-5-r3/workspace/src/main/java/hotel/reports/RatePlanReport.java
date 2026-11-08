package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** How many reservations, and how much money kept, each rate plan brought in a month (CSV). */
public final class RatePlanReport extends Report<YearMonth> {

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
    protected List<String> lines(YearMonth month) {
        List<Reservation> reservations = MonthReservations.withRevenue(store, month);
        List<String> lines = new ArrayList<>();
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = reservations.stream().filter(r -> r.ratePlan() == plan).toList();
            lines.add(plan + "," + ofPlan.size() + "," + amount(ofPlan).toPlainString());
        }
        lines.add("total," + reservations.size() + "," + amount(reservations).toPlainString());
        return lines;
    }

    private static BigDecimal amount(List<Reservation> reservations) {
        return reservations.stream().map(Reservation::amountKept).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}
