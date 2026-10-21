package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/**
 * Revenue by rate plan: how much money the inn keeps from each rate plan in a given month, for the
 * accountant (CSV).
 */
public final class RatePlansReport {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        Map<RatePlan, Integer> reservationCount = new HashMap<>();
        Map<RatePlan, BigDecimal> revenuePerPlan = new HashMap<>();

        for (RatePlan plan : RatePlan.values()) {
            reservationCount.put(plan, 0);
            revenuePerPlan.put(plan, BigDecimal.ZERO.setScale(2));
        }

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                RatePlan plan = reservation.ratePlan();
                reservationCount.put(plan, reservationCount.get(plan) + 1);
                revenuePerPlan.put(plan,
                        revenuePerPlan.get(plan).add(reservation.amountKept()));
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append('\n');
        csv.append("rate_plan,reservations,amount\n");

        int totalReservations = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO.setScale(2);
        for (RatePlan plan : RatePlan.values()) {
            int count = reservationCount.get(plan);
            BigDecimal amount = revenuePerPlan.get(plan);
            csv.append(plan.name()).append(',')
                    .append(count).append(',')
                    .append(amount.toPlainString()).append('\n');
            totalReservations += count;
            totalRevenue = totalRevenue.add(amount);
        }

        csv.append("total,").append(totalReservations).append(',')
                .append(totalRevenue.toPlainString()).append('\n');
        return csv.toString();
    }
}
