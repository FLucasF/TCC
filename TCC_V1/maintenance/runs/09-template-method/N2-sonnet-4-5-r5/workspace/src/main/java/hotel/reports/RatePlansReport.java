package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;

/** How much each rate plan brought to the inn, from reservations starting in the month. */
public final class RatePlansReport extends Report {

    private final ReservationStore store;
    private Map<RatePlan, PlanStats> statsByPlan;
    private BigDecimal totalAmount;
    private int totalReservations;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        this.statsByPlan = new EnumMap<>(RatePlan.class);
        for (RatePlan plan : RatePlan.values()) {
            statsByPlan.put(plan, new PlanStats());
        }

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                PlanStats stats = statsByPlan.get(reservation.ratePlan());
                stats.reservations++;
                stats.amount = stats.amount.add(reservation.amountKept());
            }
        }

        this.totalReservations = 0;
        this.totalAmount = BigDecimal.ZERO.setScale(2);
        for (PlanStats stats : statsByPlan.values()) {
            totalReservations += stats.reservations;
            totalAmount = totalAmount.add(stats.amount);
        }

        return super.generate(month.toString());
    }

    @Override
    protected String reportName() {
        return "rate-plans";
    }

    @Override
    protected String header() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected String rows() {
        StringBuilder csv = new StringBuilder();
        csv.append("STANDARD,")
                .append(statsByPlan.get(RatePlan.STANDARD).reservations).append(',')
                .append(statsByPlan.get(RatePlan.STANDARD).amount.toPlainString()).append('\n');
        csv.append("NON_REFUNDABLE,")
                .append(statsByPlan.get(RatePlan.NON_REFUNDABLE).reservations).append(',')
                .append(statsByPlan.get(RatePlan.NON_REFUNDABLE).amount.toPlainString()).append('\n');
        return csv.toString();
    }

    @Override
    protected String totalLine() {
        return "total," + totalReservations + ',' + totalAmount.toPlainString();
    }

    private static final class PlanStats {
        int reservations = 0;
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
    }
}
