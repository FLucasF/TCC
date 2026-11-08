package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import hotel.reservations.RatePlan;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Revenue breakdown by rate plan (CSV). */
public final class RatePlanReport {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        int standardCount = 0;
        int nonRefundableCount = 0;
        BigDecimal standardAmount = BigDecimal.ZERO.setScale(2);
        BigDecimal nonRefundableAmount = BigDecimal.ZERO.setScale(2);

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                if (reservation.ratePlan() == RatePlan.STANDARD) {
                    standardCount++;
                    standardAmount = standardAmount.add(reservation.amountKept());
                } else {
                    nonRefundableCount++;
                    nonRefundableAmount = nonRefundableAmount.add(reservation.amountKept());
                }
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("rate_plan,reservations,amount\n");
        csv.append("STANDARD,").append(standardCount).append(',')
                .append(standardAmount.toPlainString()).append('\n');
        csv.append("NON_REFUNDABLE,").append(nonRefundableCount).append(',')
                .append(nonRefundableAmount.toPlainString()).append('\n');
        BigDecimal total = standardAmount.add(nonRefundableAmount);
        csv.append("total,").append(standardCount + nonRefundableCount).append(',')
                .append(total.toPlainString()).append('\n');
        return ReportFormatter.withTitle("rate-plans", month, csv.toString());
    }
}
