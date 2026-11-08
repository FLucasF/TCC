package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class RatePlansReport extends CsvReport {

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

        StringBuilder rows = new StringBuilder();
        int totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

        for (RatePlan plan : RatePlan.values()) {
            int reservationCount = 0;
            BigDecimal planAmount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : withRevenue) {
                if (reservation.ratePlan() == plan) {
                    reservationCount++;
                    planAmount = planAmount.add(reservation.amountKept());
                }
            }
            rows.append(plan).append(',')
                    .append(reservationCount).append(',')
                    .append(planAmount.toPlainString()).append('\n');
            totalReservations += reservationCount;
            totalAmount = totalAmount.add(planAmount);
        }

        String total = "total," + totalReservations + "," + totalAmount.toPlainString() + "\n";

        return formatCsv("rate-plans", month.toString(), "rate_plan,reservations,amount", rows.toString(), total);
    }
}
