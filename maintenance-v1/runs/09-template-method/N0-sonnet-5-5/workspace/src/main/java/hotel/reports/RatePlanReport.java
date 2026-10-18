package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;

/** How many reservations, and how much money kept, each rate plan brought in a given month (CSV). */
public final class RatePlanReport {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        StringBuilder csv = new StringBuilder(ReportTitle.line("rate-plans", month));
        csv.append("rate_plan,reservations,amount\n");
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (RatePlan plan : RatePlan.values()) {
            int count = 0;
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : store.all()) {
                if (reservation.ratePlan() == plan && RevenueReport.counts(reservation, month)) {
                    count++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            csv.append(plan).append(',').append(count).append(',').append(amount.toPlainString()).append('\n');
            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }
        csv.append("total,").append(totalCount).append(',').append(totalAmount.toPlainString()).append('\n');
        return csv.toString();
    }
}
