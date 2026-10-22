package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import hotel.reservations.RatePlan;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Revenue by rate plan (STANDARD and NON_REFUNDABLE) for the month. */
public final class RatePlanReport extends Report {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("rate_plan,reservations,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        int totalReservations = 0;

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> forPlan = new ArrayList<>();
            for (Reservation reservation : store.all()) {
                boolean keptMoney = reservation.amountKept().signum() > 0;
                boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
                if (reservation.ratePlan() == plan && keptMoney && startsInMonth) {
                    forPlan.add(reservation);
                }
            }

            int count = forPlan.size();
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation r : forPlan) {
                amount = amount.add(r.amountKept());
            }

            csv.append(plan).append(',')
                    .append(count).append(',')
                    .append(amount.toPlainString()).append('\n');

            totalReservations += count;
            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,").append(totalReservations).append(',')
                .append(totalAmount.toPlainString()).append('\n');
        return buildCsv("rate-plans", month.toString(), csv);
    }
}
