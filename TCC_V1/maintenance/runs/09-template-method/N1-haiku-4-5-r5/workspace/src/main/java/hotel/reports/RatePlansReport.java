package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Which rate plans bring in the most money from the reservations that start in a given month,
 * for the management (CSV).
 */
public final class RatePlansReport {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<Reservation> profitable = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                profitable.add(reservation);
            }
        }

        BigDecimal standardAmount = BigDecimal.ZERO.setScale(2);
        BigDecimal nonRefundableAmount = BigDecimal.ZERO.setScale(2);
        int standardCount = 0;
        int nonRefundableCount = 0;

        for (Reservation reservation : profitable) {
            if (reservation.ratePlan() == RatePlan.STANDARD) {
                standardCount++;
                standardAmount = standardAmount.add(reservation.amountKept());
            } else if (reservation.ratePlan() == RatePlan.NON_REFUNDABLE) {
                nonRefundableCount++;
                nonRefundableAmount = nonRefundableAmount.add(reservation.amountKept());
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append('\n');
        csv.append("rate_plan,reservations,amount\n");
        csv.append("STANDARD,").append(standardCount).append(',')
                .append(standardAmount.toPlainString()).append('\n');
        csv.append("NON_REFUNDABLE,").append(nonRefundableCount).append(',')
                .append(nonRefundableAmount.toPlainString()).append('\n');
        BigDecimal totalAmount = standardAmount.add(nonRefundableAmount);
        int totalCount = standardCount + nonRefundableCount;
        csv.append("total,").append(totalCount).append(',')
                .append(totalAmount.toPlainString()).append('\n');
        return csv.toString();
    }
}
