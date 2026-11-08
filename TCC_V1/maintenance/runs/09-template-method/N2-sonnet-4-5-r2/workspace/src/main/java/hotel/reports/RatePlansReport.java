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

/** Revenue broken down by rate plan, for management (CSV). */
public final class RatePlansReport extends ReportTemplate<YearMonth> {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    @Override
    protected String reportName() {
        return "rate-plans";
    }

    @Override
    protected String headerLine() {
        return "rate_plan,reservations,amount\n";
    }

    @Override
    protected String dataLines(YearMonth month) {
        List<Reservation> withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                withRevenue.add(reservation);
            }
        }

        Map<RatePlan, PlanSummary> summaries = new LinkedHashMap<>();
        summaries.put(RatePlan.STANDARD, new PlanSummary());
        summaries.put(RatePlan.NON_REFUNDABLE, new PlanSummary());

        for (Reservation reservation : withRevenue) {
            PlanSummary summary = summaries.get(reservation.ratePlan());
            summary.count++;
            summary.amount = summary.amount.add(reservation.amountKept());
        }

        StringBuilder lines = new StringBuilder();
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (Map.Entry<RatePlan, PlanSummary> entry : summaries.entrySet()) {
            PlanSummary summary = entry.getValue();
            lines.append(entry.getKey()).append(',')
                    .append(summary.count).append(',')
                    .append(summary.amount.toPlainString()).append('\n');
            totalCount += summary.count;
            totalAmount = totalAmount.add(summary.amount);
        }
        this.totalReservations = totalCount;
        this.totalAmount = totalAmount;
        return lines.toString();
    }

    @Override
    protected String totalLine(YearMonth month) {
        return "total," + totalReservations + ',' + totalAmount.toPlainString() + '\n';
    }

    private int totalReservations;
    private BigDecimal totalAmount;

    private static final class PlanSummary {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
    }
}
