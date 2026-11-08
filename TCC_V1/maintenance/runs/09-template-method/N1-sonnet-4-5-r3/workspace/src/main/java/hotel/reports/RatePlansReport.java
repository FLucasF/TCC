package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;

/**
 * The money each rate plan brought to the inn from the reservations that start in a given month,
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
        csv.append("rate_plan,reservations,amount\n");
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (RatePlan plan : RatePlan.values()) {
            int count = counts.get(plan);
            BigDecimal amount = amounts.get(plan);
            csv.append(plan).append(',')
                    .append(count).append(',')
                    .append(amount.toPlainString()).append('\n');
            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }
        csv.append("total,").append(totalCount).append(',').append(totalAmount.toPlainString()).append('\n');
        return CsvReport.withTitle("rate-plans", month.toString(), csv.toString());
    }
}
