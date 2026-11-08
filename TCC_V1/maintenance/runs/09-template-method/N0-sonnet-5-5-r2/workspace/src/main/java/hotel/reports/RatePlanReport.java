package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** The reservations and the money kept per rate plan, over the reservations the revenue report counts (CSV). */
public final class RatePlanReport {

    private final RevenueReport revenue;

    public RatePlanReport(RevenueReport revenue) {
        this.revenue = revenue;
    }

    public String generate(YearMonth month) {
        List<Reservation> counted = revenue.reservationsWithRevenue(month);
        List<String> rows = new ArrayList<>();
        int totalReservations = 0;
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
            rows.add(plan + "," + count + "," + amount.toPlainString());
            totalReservations += count;
            totalAmount = totalAmount.add(amount);
        }
        return ReportLayout.render("rate-plans", month, "rate_plan,reservations,amount", rows,
                "total," + totalReservations + "," + totalAmount.toPlainString());
    }
}
