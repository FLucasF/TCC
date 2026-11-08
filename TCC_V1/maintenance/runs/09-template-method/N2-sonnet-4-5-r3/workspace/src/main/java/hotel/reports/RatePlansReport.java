package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;

/**
 * How much money each rate plan brings in: counts the same reservations as the revenue report,
 * grouped by plan (CSV).
 */
public final class RatePlansReport extends Report {

    private final ReservationStore store;
    private Map<RatePlan, BigDecimal> amountByPlan;
    private Map<RatePlan, Integer> countByPlan;
    private int totalCount;
    private BigDecimal totalAmount;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        this.amountByPlan = new EnumMap<>(RatePlan.class);
        this.countByPlan = new EnumMap<>(RatePlan.class);
        for (RatePlan plan : RatePlan.values()) {
            amountByPlan.put(plan, BigDecimal.ZERO.setScale(2));
            countByPlan.put(plan, 0);
        }

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                RatePlan plan = reservation.ratePlan();
                amountByPlan.put(plan, amountByPlan.get(plan).add(reservation.amountKept()));
                countByPlan.put(plan, countByPlan.get(plan) + 1);
            }
        }

        this.totalCount = 0;
        this.totalAmount = BigDecimal.ZERO.setScale(2);
        for (RatePlan plan : RatePlan.values()) {
            totalCount += countByPlan.get(plan);
            totalAmount = totalAmount.add(amountByPlan.get(plan));
        }

        return super.generate(month.toString());
    }

    @Override
    protected String reportName() {
        return "rate-plans";
    }

    @Override
    protected String header() {
        return "rate_plan,reservations,amount\n";
    }

    @Override
    protected String dataRows() {
        StringBuilder rows = new StringBuilder();
        rows.append("STANDARD,")
                .append(countByPlan.get(RatePlan.STANDARD)).append(',')
                .append(amountByPlan.get(RatePlan.STANDARD).toPlainString()).append('\n');
        rows.append("NON_REFUNDABLE,")
                .append(countByPlan.get(RatePlan.NON_REFUNDABLE)).append(',')
                .append(amountByPlan.get(RatePlan.NON_REFUNDABLE).toPlainString()).append('\n');
        return rows.toString();
    }

    @Override
    protected String totalRow() {
        return "total," + totalCount + ',' + totalAmount.toPlainString() + '\n';
    }
}
