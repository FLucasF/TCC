package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Revenue by rate plan for reservations starting in a given month, for management review (CSV).
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
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                withRevenue.add(reservation);
            }
        }

        Map<RatePlan, PlanStats> stats = new LinkedHashMap<>();
        stats.put(RatePlan.STANDARD, new PlanStats());
        stats.put(RatePlan.NON_REFUNDABLE, new PlanStats());

        for (Reservation reservation : withRevenue) {
            PlanStats planStats = stats.get(reservation.ratePlan());
            planStats.count++;
            planStats.revenue = planStats.revenue.add(reservation.amountKept());
        }

        StringBuilder csv = new StringBuilder();
        csv.append(ReportFormat.titleLine("rate-plans", month.toString()));
        csv.append("rate_plan,reservations,amount\n");
        int totalCount = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO.setScale(2);
        for (Map.Entry<RatePlan, PlanStats> entry : stats.entrySet()) {
            PlanStats planStats = entry.getValue();
            csv.append(entry.getKey()).append(',')
                    .append(planStats.count).append(',')
                    .append(planStats.revenue.toPlainString()).append('\n');
            totalCount += planStats.count;
            totalRevenue = totalRevenue.add(planStats.revenue);
        }
        csv.append("total,").append(totalCount).append(',').append(totalRevenue.toPlainString()).append('\n');
        return csv.toString();
    }

    private static final class PlanStats {
        int count = 0;
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
    }
}
