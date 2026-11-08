package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/**
 * Which rate plan brings in the most money from the reservations that start in a given month,
 * for management (CSV).
 */
public final class RatePlansReport {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        Map<RatePlan, BigDecimal> amounts = new HashMap<>();
        Map<RatePlan, Integer> counts = new HashMap<>();
        for (RatePlan plan : RatePlan.values()) {
            amounts.put(plan, BigDecimal.ZERO.setScale(2));
            counts.put(plan, 0);
        }

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                RatePlan plan = reservation.ratePlan();
                amounts.put(plan, amounts.get(plan).add(reservation.amountKept()));
                counts.put(plan, counts.get(plan) + 1);
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append('\n');
        csv.append("rate_plan,reservations,amount\n");
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        int totalCount = 0;
        for (RatePlan plan : RatePlan.values()) {
            csv.append(plan).append(',')
                    .append(counts.get(plan)).append(',')
                    .append(amounts.get(plan).toPlainString()).append('\n');
            totalAmount = totalAmount.add(amounts.get(plan));
            totalCount += counts.get(plan);
        }
        csv.append("total,").append(totalCount).append(',').append(totalAmount.toPlainString()).append('\n');
        return csv.toString();
    }
}
