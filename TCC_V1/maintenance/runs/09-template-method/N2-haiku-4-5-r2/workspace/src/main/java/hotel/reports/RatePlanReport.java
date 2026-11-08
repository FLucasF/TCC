package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import hotel.reservations.RatePlan;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Revenue by rate plan for reservations starting in a given month (CSV). */
public final class RatePlanReport {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<Reservation> reservations = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                reservations.add(reservation);
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        int totalReservations = 0;

        for (RatePlan plan : RatePlan.values()) {
            int count = 0;
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : reservations) {
                if (reservation.ratePlan() == plan) {
                    count++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            csv.append(plan).append(',')
                    .append(count).append(',')
                    .append(amount.toPlainString()).append('\n');
            totalReservations += count;
            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,").append(totalReservations).append(',').append(totalAmount.toPlainString()).append('\n');
        return csv.toString();
    }
}
