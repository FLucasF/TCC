package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/**
 * Revenue breakdown by rate plan for reservations starting in a given month, for management (CSV).
 */
public final class RatePlansReport {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        Map<RatePlan, PlanStats> stats = new HashMap<>();
        stats.put(RatePlan.STANDARD, new PlanStats());
        stats.put(RatePlan.NON_REFUNDABLE, new PlanStats());

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                PlanStats planStats = stats.get(reservation.ratePlan());
                planStats.count++;
                planStats.amount = planStats.amount.add(reservation.amountKept());
            }
        }

        StringBuilder content = new StringBuilder();
        content.append("rate_plan,reservations,amount\n");

        PlanStats standard = stats.get(RatePlan.STANDARD);
        content.append("STANDARD,")
                .append(standard.count).append(',')
                .append(standard.amount.toPlainString()).append('\n');

        PlanStats nonRefundable = stats.get(RatePlan.NON_REFUNDABLE);
        content.append("NON_REFUNDABLE,")
                .append(nonRefundable.count).append(',')
                .append(nonRefundable.amount.toPlainString()).append('\n');

        int totalCount = standard.count + nonRefundable.count;
        BigDecimal totalAmount = standard.amount.add(nonRefundable.amount);
        content.append("total,")
                .append(totalCount).append(',')
                .append(totalAmount.toPlainString()).append('\n');

        return Report.withTitle("rate-plans", month.toString(), content.toString());
    }

    private static class PlanStats {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
    }
}
