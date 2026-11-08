package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Revenue by rate plan for reservations that start in a given month, for management (CSV).
 * Shows which rate plan brings more money.
 */
public final class RatePlansReport {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        Map<RatePlan, PlanStats> statsByPlan = new EnumMap<>(RatePlan.class);
        for (RatePlan plan : RatePlan.values()) {
            statsByPlan.put(plan, new PlanStats());
        }

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                PlanStats stats = statsByPlan.get(reservation.ratePlan());
                stats.count++;
                stats.amount = stats.amount.add(reservation.amountKept());
            }
        }

        StringBuilder dataLines = new StringBuilder();
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

        // STANDARD first, then NON_REFUNDABLE
        for (RatePlan plan : RatePlan.values()) {
            PlanStats stats = statsByPlan.get(plan);
            dataLines.append(plan).append(',')
                    .append(stats.count).append(',')
                    .append(stats.amount.toPlainString()).append('\n');
            totalCount += stats.count;
            totalAmount = totalAmount.add(stats.amount);
        }

        String header = "rate_plan,reservations,amount";
        String totalLine = "total," + totalCount + "," + totalAmount.toPlainString();
        return new CsvReport("rate-plans", month.toString()).format(header, dataLines.toString(), totalLine);
    }

    private static final class PlanStats {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
    }
}
