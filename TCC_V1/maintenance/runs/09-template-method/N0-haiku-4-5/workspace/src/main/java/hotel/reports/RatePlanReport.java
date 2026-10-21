package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import hotel.reservations.RatePlan;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class RatePlanReport {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<Reservation> paid = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean isPaid = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (isPaid && startsInMonth) {
                paid.add(reservation);
            }
        }

        long standardCount = 0;
        BigDecimal standardAmount = BigDecimal.ZERO.setScale(2);
        long nonRefundableCount = 0;
        BigDecimal nonRefundableAmount = BigDecimal.ZERO.setScale(2);

        for (Reservation reservation : paid) {
            if (reservation.ratePlan() == RatePlan.STANDARD) {
                standardCount++;
                standardAmount = standardAmount.add(reservation.amountKept());
            } else if (reservation.ratePlan() == RatePlan.NON_REFUNDABLE) {
                nonRefundableCount++;
                nonRefundableAmount = nonRefundableAmount.add(reservation.amountKept());
            }
        }

        long totalCount = standardCount + nonRefundableCount;
        BigDecimal totalAmount = standardAmount.add(nonRefundableAmount);

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append('\n');
        csv.append("rate_plan,reservations,amount\n");
        csv.append("STANDARD,").append(standardCount).append(',').append(standardAmount.toPlainString()).append('\n');
        csv.append("NON_REFUNDABLE,").append(nonRefundableCount).append(',').append(nonRefundableAmount.toPlainString()).append('\n');
        csv.append("total,").append(totalCount).append(',').append(totalAmount.toPlainString()).append('\n');
        return csv.toString();
    }
}
