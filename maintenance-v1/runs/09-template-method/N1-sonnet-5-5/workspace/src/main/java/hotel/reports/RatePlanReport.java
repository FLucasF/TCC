package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Which rate plan brings the inn more money: the reservations the revenue report counts, per plan (CSV). */
public final class RatePlanReport extends CsvReport<YearMonth> {

    private final RevenueReport revenue;

    public RatePlanReport(RevenueReport revenue) {
        super("rate-plans", PeriodKinds.MONTH);
        this.revenue = revenue;
    }

    @Override
    String body(YearMonth month) {
        List<Reservation> withRevenue = revenue.reservationsWithRevenue(month);

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
