package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Revenue by rate plan report for paid reservations starting in a given month. */
public final class RatePlanReport {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<Reservation> withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                withRevenue.add(reservation);
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append('\n');
        csv.append("rate_plan,reservations,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        int totalReservations = 0;

        for (RatePlan plan : RatePlan.values()) {
            int reservationCount = 0;
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : withRevenue) {
                if (reservation.ratePlan() == plan) {
                    reservationCount++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            csv.append(plan).append(',')
                    .append(reservationCount).append(',')
                    .append(amount.toPlainString()).append('\n');
            totalReservations += reservationCount;
            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,").append(totalReservations).append(',')
                .append(totalAmount.toPlainString()).append('\n');
        return csv.toString();
    }
}
