package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;

/** Revenue by rate plan for paid reservations starting in a given month (CSV). */
public final class RatePlanReport {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        Map<RatePlan, ReservationCount> counts = new EnumMap<>(RatePlan.class);
        counts.put(RatePlan.STANDARD, new ReservationCount());
        counts.put(RatePlan.NON_REFUNDABLE, new ReservationCount());

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                ReservationCount count = counts.get(reservation.ratePlan());
                count.reservations++;
                count.amount = count.amount.add(reservation.amountKept());
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append('\n');
        csv.append("rate_plan,reservations,amount\n");
        int totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (RatePlan plan : RatePlan.values()) {
            ReservationCount count = counts.get(plan);
            csv.append(plan).append(',')
                    .append(count.reservations).append(',')
                    .append(count.amount.toPlainString()).append('\n');
            totalReservations += count.reservations;
            totalAmount = totalAmount.add(count.amount);
        }
        csv.append("total,").append(totalReservations).append(',').append(totalAmount.toPlainString()).append('\n');
        return csv.toString();
    }

    private static class ReservationCount {
        int reservations = 0;
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
    }
}
