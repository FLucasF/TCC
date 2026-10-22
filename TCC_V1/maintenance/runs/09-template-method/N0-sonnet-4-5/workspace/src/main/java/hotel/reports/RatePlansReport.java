package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;

/** How much money each rate plan brought to the inn in a given month, for management (CSV). */
public final class RatePlansReport extends CsvReport {

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

        return formatCsv("rate-plans", month.toString(), csv -> {
            csv.append("rate_plan,reservations,amount\n");
            int totalReservations = 0;
            BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

            csv.append("STANDARD,")
                    .append(counts.get(RatePlan.STANDARD)).append(',')
                    .append(amounts.get(RatePlan.STANDARD).toPlainString()).append('\n');
            totalReservations += counts.get(RatePlan.STANDARD);
            totalAmount = totalAmount.add(amounts.get(RatePlan.STANDARD));

            csv.append("NON_REFUNDABLE,")
                    .append(counts.get(RatePlan.NON_REFUNDABLE)).append(',')
                    .append(amounts.get(RatePlan.NON_REFUNDABLE).toPlainString()).append('\n');
            totalReservations += counts.get(RatePlan.NON_REFUNDABLE);
            totalAmount = totalAmount.add(amounts.get(RatePlan.NON_REFUNDABLE));

            csv.append("total,").append(totalReservations).append(',')
                    .append(totalAmount.toPlainString()).append('\n');
            return csv;
        });
    }
}
