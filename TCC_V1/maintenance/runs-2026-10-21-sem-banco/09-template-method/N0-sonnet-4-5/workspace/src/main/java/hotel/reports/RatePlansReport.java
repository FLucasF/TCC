package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/** Revenue breakdown by rate plan for reservations starting in a given month (CSV). */
public final class RatePlansReport extends Report {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        Map<RatePlan, Integer> reservationCounts = new HashMap<>();
        Map<RatePlan, BigDecimal> amounts = new HashMap<>();

        for (RatePlan plan : RatePlan.values()) {
            reservationCounts.put(plan, 0);
            amounts.put(plan, BigDecimal.ZERO.setScale(2));
        }

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                RatePlan plan = reservation.ratePlan();
                reservationCounts.put(plan, reservationCounts.get(plan) + 1);
                amounts.put(plan, amounts.get(plan).add(reservation.amountKept()));
            }
        }

        StringBuilder rows = new StringBuilder();
        rows.append("rate_plan,reservations,amount\n");

        int totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

        rows.append("STANDARD,")
                .append(reservationCounts.get(RatePlan.STANDARD)).append(',')
                .append(amounts.get(RatePlan.STANDARD).toPlainString()).append('\n');

        rows.append("NON_REFUNDABLE,")
                .append(reservationCounts.get(RatePlan.NON_REFUNDABLE)).append(',')
                .append(amounts.get(RatePlan.NON_REFUNDABLE).toPlainString()).append('\n');

        for (RatePlan plan : RatePlan.values()) {
            totalReservations += reservationCounts.get(plan);
            totalAmount = totalAmount.add(amounts.get(plan));
        }

        rows.append("total,")
                .append(totalReservations).append(',')
                .append(totalAmount.toPlainString()).append('\n');

        return buildCsv("rate-plans", month.toString(), rows.toString());
    }
}
