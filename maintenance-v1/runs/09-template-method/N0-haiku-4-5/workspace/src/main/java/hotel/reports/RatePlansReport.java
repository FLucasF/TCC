package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Revenue breakdown by rate plan (CSV). */
public final class RatePlansReport {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        BigDecimal standardAmount = BigDecimal.ZERO.setScale(2);
        BigDecimal nonRefundableAmount = BigDecimal.ZERO.setScale(2);
        int standardReservations = 0;
        int nonRefundableReservations = 0;

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                if (reservation.ratePlan() == RatePlan.STANDARD) {
                    standardReservations++;
                    standardAmount = standardAmount.add(reservation.amountKept());
                } else if (reservation.ratePlan() == RatePlan.NON_REFUNDABLE) {
                    nonRefundableReservations++;
                    nonRefundableAmount = nonRefundableAmount.add(reservation.amountKept());
                }
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append('\n');
        csv.append("rate_plan,reservations,amount\n");
        csv.append("STANDARD,").append(standardReservations).append(',')
                .append(standardAmount.toPlainString()).append('\n');
        csv.append("NON_REFUNDABLE,").append(nonRefundableReservations).append(',')
                .append(nonRefundableAmount.toPlainString()).append('\n');
        BigDecimal totalAmount = standardAmount.add(nonRefundableAmount);
        int totalReservations = standardReservations + nonRefundableReservations;
        csv.append("total,").append(totalReservations).append(',')
                .append(totalAmount.toPlainString()).append('\n');
        return csv.toString();
    }
}
