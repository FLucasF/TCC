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

/** Shows which rate plan brings more money from reservations that start in a given month (CSV). */
public final class RatePlansReport extends Report {

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
    protected String header() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected String dataLines() {
        Map<RatePlan, PlanStats> statsByPlan = calculateStats();

        StringBuilder lines = new StringBuilder();
        lines.append(RatePlan.STANDARD).append(',')
                .append(statsByPlan.get(RatePlan.STANDARD).count).append(',')
                .append(statsByPlan.get(RatePlan.STANDARD).revenue.toPlainString()).append('\n');
        lines.append(RatePlan.NON_REFUNDABLE).append(',')
                .append(statsByPlan.get(RatePlan.NON_REFUNDABLE).count).append(',')
                .append(statsByPlan.get(RatePlan.NON_REFUNDABLE).revenue.toPlainString()).append('\n');
        return lines.toString();
    }

    @Override
    protected String totalLine() {
        Map<RatePlan, PlanStats> statsByPlan = calculateStats();
        int totalCount = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO.setScale(2);
        for (PlanStats stats : statsByPlan.values()) {
            totalCount += stats.count;
            totalRevenue = totalRevenue.add(stats.revenue);
        }
        return "total," + totalCount + ',' + totalRevenue.toPlainString();
    }

    private Map<RatePlan, PlanStats> calculateStats() {
        Map<RatePlan, PlanStats> statsByPlan = new EnumMap<>(RatePlan.class);
        statsByPlan.put(RatePlan.STANDARD, new PlanStats());
        statsByPlan.put(RatePlan.NON_REFUNDABLE, new PlanStats());

        List<Reservation> withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                withRevenue.add(reservation);
            }
        }

        for (Reservation reservation : withRevenue) {
            PlanStats stats = statsByPlan.get(reservation.ratePlan());
            stats.count++;
            stats.revenue = stats.revenue.add(reservation.amountKept());
        }

        return statsByPlan;
    }

    private static class PlanStats {
        int count = 0;
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
    }
}
