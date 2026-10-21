package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * The money each rate plan brought in from the reservations that start in a given month,
 * for management (CSV). Counts the same reservations the revenue report counts.
 */
public final class RatePlansReport {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
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

        CsvReport report = new CsvReport("rate-plans", month.toString(),
                List.of("rate_plan", "reservations", "amount"));

        int totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

        for (RatePlan plan : RatePlan.values()) {
            int count = 0;
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : withRevenue) {
                if (reservation.ratePlan() == plan) {
                    count++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            report.addRow(plan.toString(), String.valueOf(count), amount.toPlainString());
            totalReservations += count;
            totalAmount = totalAmount.add(amount);
        }

        report.setTotal("total", String.valueOf(totalReservations), totalAmount.toPlainString());
        return report.render();
    }
}
