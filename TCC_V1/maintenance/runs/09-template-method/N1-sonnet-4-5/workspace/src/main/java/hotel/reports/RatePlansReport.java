package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Which rate plan brings more money, for management (CSV). */
public final class RatePlansReport {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<RatePlanSummary> summaries = new ArrayList<>();
        for (RatePlan plan : RatePlan.values()) {
            summaries.add(summarize(plan, month));
        }

        StringBuilder csv = new StringBuilder();
        csv.append("rate_plan,reservations,amount\n");
        int totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (RatePlanSummary summary : summaries) {
            csv.append(summary.plan).append(',')
                    .append(summary.count).append(',')
                    .append(summary.amount.toPlainString()).append('\n');
            totalReservations += summary.count;
            totalAmount = totalAmount.add(summary.amount);
        }
        csv.append("total,").append(totalReservations).append(',').append(totalAmount.toPlainString()).append('\n');
        return ReportFormatter.withTitle("rate-plans", month.toString(), csv.toString());
    }

    private RatePlanSummary summarize(RatePlan plan, YearMonth month) {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            boolean matchesPlan = reservation.ratePlan() == plan;
            if (keptMoney && startsInMonth && matchesPlan) {
                count++;
                amount = amount.add(reservation.amountKept());
            }
        }
        return new RatePlanSummary(plan, count, amount);
    }

    private record RatePlanSummary(RatePlan plan, int count, BigDecimal amount) {}
}
