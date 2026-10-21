package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** How many reservations and how much money each rate plan brought in a given month (CSV), for management. */
public final class RatePlanReport {

    private static final List<RatePlan> PLANS = List.of(RatePlan.STANDARD, RatePlan.NON_REFUNDABLE);

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<Reservation> counted = MonthlyReservations.startingIn(month, store, MonthlyReservations::keptMoney);
        List<String> rows = new ArrayList<>();
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (RatePlan plan : PLANS) {
            int count = 0;
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : counted) {
                if (reservation.ratePlan() == plan) {
                    count++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            rows.add(plan + "," + count + "," + amount.toPlainString());
            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }
        return CsvLayout.render("rate-plans", month.toString(), "rate_plan,reservations,amount", rows,
                "total," + totalCount + "," + totalAmount.toPlainString());
    }
}
