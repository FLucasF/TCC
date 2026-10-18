package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** How many reservations, and how much money kept, each rate plan brought in a month (CSV). */
public final class RatePlansReport extends Report<YearMonth> {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "rate-plans";
    }

    @Override
    protected String body(YearMonth month) {
        List<Reservation> withRevenue = RevenueReport.withRevenueIn(store, month);

        StringBuilder csv = new StringBuilder();
        csv.append("rate_plan,reservations,amount\n");
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
            csv.append(plan).append(',').append(count).append(',').append(amount.toPlainString()).append('\n');
            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }
        csv.append("total,").append(totalCount).append(',').append(totalAmount.toPlainString()).append('\n');
        return csv.toString();
    }
}
