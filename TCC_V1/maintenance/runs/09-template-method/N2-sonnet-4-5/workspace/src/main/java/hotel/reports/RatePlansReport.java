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

/** Revenue grouped by rate plan, for management (CSV). */
public final class RatePlansReport extends CsvReport {

    private final ReservationStore store;
    private YearMonth month;
    private Map<RatePlan, PlanStats> stats;
    private int totalReservations;
    private BigDecimal totalAmount;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        this.month = month;
        this.stats = new EnumMap<>(RatePlan.class);
        for (RatePlan plan : RatePlan.values()) {
            stats.put(plan, new PlanStats());
        }

        List<Reservation> withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                withRevenue.add(reservation);
            }
        }

        for (Reservation reservation : withRevenue) {
            PlanStats planStats = stats.get(reservation.ratePlan());
            planStats.count++;
            planStats.amount = planStats.amount.add(reservation.amountKept());
        }

        this.totalReservations = 0;
        this.totalAmount = BigDecimal.ZERO.setScale(2);
        for (PlanStats planStats : stats.values()) {
            totalReservations += planStats.count;
            totalAmount = totalAmount.add(planStats.amount);
        }

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
    protected String dataRows() {
        StringBuilder rows = new StringBuilder();
        for (RatePlan plan : RatePlan.values()) {
            PlanStats planStats = stats.get(plan);
            rows.append(plan).append(',')
                    .append(planStats.count).append(',')
                    .append(planStats.amount.toPlainString()).append('\n');
        }
        return rows.toString();
    }

    @Override
    protected String totalRow() {
        return "total," + totalReservations + ',' + totalAmount.toPlainString();
    }

    private static final class PlanStats {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
    }
}
