package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** How many reservations and how much money each rate plan brought in a given month, for management (CSV). */
public final class RatePlanReport extends CsvReport<YearMonth> {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        super("rate-plans");
        this.store = store;
    }

    @Override
    String header() {
        return "rate_plan,reservations,amount";
    }

    @Override
    List<String> lines(YearMonth month) {
        List<Reservation> withRevenue = MonthReservations.withRevenue(store, month);
        List<String> lines = new ArrayList<>();
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (RatePlan plan : RatePlan.values()) {
            int count = 0;
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : withRevenue) {
                if (reservation.ratePlan() == plan) {
                    count++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            lines.add(plan + "," + count + "," + amount.toPlainString());
            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }
        lines.add("total," + totalCount + "," + totalAmount.toPlainString());
        return lines;
    }
}
