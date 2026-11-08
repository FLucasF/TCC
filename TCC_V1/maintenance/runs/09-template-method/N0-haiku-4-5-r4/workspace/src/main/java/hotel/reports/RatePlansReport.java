package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/**
 * The money brought by each rate plan from the reservations that start in a given month, for management
 * (CSV).
 */
public final class RatePlansReport {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        Map<RatePlan, Integer> countByPlan = new HashMap<>();
        Map<RatePlan, BigDecimal> amountByPlan = new HashMap<>();

        countByPlan.put(RatePlan.STANDARD, 0);
        countByPlan.put(RatePlan.NON_REFUNDABLE, 0);
        amountByPlan.put(RatePlan.STANDARD, BigDecimal.ZERO.setScale(2));
        amountByPlan.put(RatePlan.NON_REFUNDABLE, BigDecimal.ZERO.setScale(2));

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                RatePlan plan = reservation.ratePlan();
                countByPlan.put(plan, countByPlan.get(plan) + 1);
                amountByPlan.put(plan, amountByPlan.get(plan).add(reservation.amountKept()));
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append('\n');
        csv.append("rate_plan,reservations,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        int totalReservations = 0;

        for (RatePlan plan : RatePlan.values()) {
            int count = countByPlan.get(plan);
            BigDecimal amount = amountByPlan.get(plan);
            csv.append(plan.name()).append(',')
                    .append(count).append(',')
                    .append(amount.toPlainString()).append('\n');
            totalReservations += count;
            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,").append(totalReservations).append(',').append(totalAmount.toPlainString()).append('\n');
        return csv.toString();
    }
}
