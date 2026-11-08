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
    public String periodParam() {
        return "month";
    }

    @Override
    public YearMonth parsePeriod(String text) {
        return parseMonth(text);
    }

    @Override
    protected List<String> lines(YearMonth month) {
        List<Reservation> reservations = MonthlyReservations.withRevenue(store, month);
        List<String> lines = new ArrayList<>();
        lines.add("rate_plan,reservations,amount");
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = reservations.stream().filter(r -> r.ratePlan() == plan).toList();
            lines.add(plan + "," + ofPlan.size() + "," + sum(ofPlan).toPlainString());
        }
        lines.add("total," + reservations.size() + "," + sum(reservations).toPlainString());
        return lines;
    }

    private static BigDecimal sum(List<Reservation> reservations) {
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : reservations) {
            sum = sum.add(reservation.amountKept());
        }
        return sum;
    }
}
