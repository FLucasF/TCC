package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * How many reservations and how much money each rate plan brought, counting the same reservations as the
 * revenue report: those that start in the month and left money with the inn (CSV).
 */
public final class RatePlanReport {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<Reservation> withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            if (keptMoney && YearMonth.from(reservation.checkIn()).equals(month)) {
                withRevenue.add(reservation);
            }
        }

        List<String> rows = new ArrayList<>();
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
            rows.add(plan + "," + count + "," + amount.toPlainString());
            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }
        return CsvReport.render("rate-plans", month.toString(), "rate_plan,reservations,amount", rows,
                "total," + totalCount + "," + totalAmount.toPlainString());
    }
}
