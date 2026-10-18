package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;

/**
 * How much revenue each rate plan brought, from reservations that start in a given month,
 * for management (CSV).
 */
public final class RatePlansReport {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        Map<RatePlan, Integer> counts = new EnumMap<>(RatePlan.class);
        Map<RatePlan, BigDecimal> amounts = new EnumMap<>(RatePlan.class);

        for (RatePlan plan : RatePlan.values()) {
            counts.put(plan, 0);
            amounts.put(plan, BigDecimal.ZERO.setScale(2));
        }

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                RatePlan plan = reservation.ratePlan();
                counts.put(plan, counts.get(plan) + 1);
                amounts.put(plan, amounts.get(plan).add(reservation.amountKept()));
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append('\n');
        csv.append("rate_plan,reservations,amount\n");

        int totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

        csv.append("STANDARD,")
                .append(counts.get(RatePlan.STANDARD)).append(',')
                .append(amounts.get(RatePlan.STANDARD).toPlainString()).append('\n');
        csv.append("NON_REFUNDABLE,")
                .append(counts.get(RatePlan.NON_REFUNDABLE)).append(',')
                .append(amounts.get(RatePlan.NON_REFUNDABLE).toPlainString()).append('\n');

        for (RatePlan plan : RatePlan.values()) {
            totalReservations += counts.get(plan);
            totalAmount = totalAmount.add(amounts.get(plan));
        }

        csv.append("total,").append(totalReservations).append(',').append(totalAmount.toPlainString()).append('\n');
        return csv.toString();
    }
}
