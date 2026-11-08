package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** How many reservations, and how much money, each rate plan brought in a month, for the management (CSV). */
public final class RatePlanReport {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<Reservation> counted = RevenueReport.withRevenue(store, month);
        CsvReport csv = new CsvReport("rate-plans", month, "rate_plan,reservations,amount");
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (RatePlan plan : RatePlan.values()) {
            int count = 0;
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : counted) {
                if (reservation.ratePlan() == plan) {
                    count++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            csv.line(plan, count, amount.toPlainString());
            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }
        csv.line("total", totalCount, totalAmount.toPlainString());
        return csv.build();
    }
}
