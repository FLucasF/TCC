package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Which rate plan brings more money, for the management (CSV). */
public final class RatePlansReport extends CsvReport {

    private final ReservationStore store;
    private YearMonth month;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        this.month = month;
        return generate();
    }

    @Override
    protected String reportName() {
        return "rate-plans";
    }

    @Override
    protected String period() {
        return month.toString();
    }

    @Override
    protected String headerLine() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> dataLines() {
        Map<RatePlan, PlanStats> statsByPlan = computeStats();
        List<String> lines = new ArrayList<>();
        lines.add(formatPlanLine(RatePlan.STANDARD, statsByPlan.get(RatePlan.STANDARD)));
        lines.add(formatPlanLine(RatePlan.NON_REFUNDABLE, statsByPlan.get(RatePlan.NON_REFUNDABLE)));
        return lines;
    }

    @Override
    protected String totalLine() {
        Map<RatePlan, PlanStats> statsByPlan = computeStats();
        int totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (PlanStats stats : statsByPlan.values()) {
            totalReservations += stats.count;
            totalAmount = totalAmount.add(stats.amount);
        }
        return "total," + totalReservations + "," + totalAmount.toPlainString();
    }

    private Map<RatePlan, PlanStats> computeStats() {
        Map<RatePlan, PlanStats> statsByPlan = new HashMap<>();
        statsByPlan.put(RatePlan.STANDARD, new PlanStats());
        statsByPlan.put(RatePlan.NON_REFUNDABLE, new PlanStats());

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                PlanStats stats = statsByPlan.get(reservation.ratePlan());
                stats.count++;
                stats.amount = stats.amount.add(reservation.amountKept());
            }
        }

        return statsByPlan;
    }

    private String formatPlanLine(RatePlan plan, PlanStats stats) {
        return plan.name() + "," + stats.count + "," + stats.amount.toPlainString();
    }

    private static class PlanStats {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
    }
}
