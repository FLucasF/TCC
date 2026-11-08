package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Revenue breakdown by rate plan for a given month, for management (CSV).
 * Shows which rate plan brings more money to the inn.
 */
public final class RatePlansReport {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        Map<RatePlan, List<Reservation>> byPlan = new EnumMap<>(RatePlan.class);
        for (RatePlan plan : RatePlan.values()) {
            byPlan.put(plan, new ArrayList<>());
        }

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                byPlan.get(reservation.ratePlan()).add(reservation);
            }
        }

        ReportFormat report = new ReportFormat("rate-plans", month.toString());
        report.appendHeader("rate_plan,reservations,amount");

        int totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        report.appendRows(csv -> {
            csv.append("STANDARD,")
                    .append(byPlan.get(RatePlan.STANDARD).size()).append(',')
                    .append(sumAmountKept(byPlan.get(RatePlan.STANDARD)).toPlainString()).append('\n');
            csv.append("NON_REFUNDABLE,")
                    .append(byPlan.get(RatePlan.NON_REFUNDABLE).size()).append(',')
                    .append(sumAmountKept(byPlan.get(RatePlan.NON_REFUNDABLE)).toPlainString()).append('\n');
        });

        for (List<Reservation> reservations : byPlan.values()) {
            totalReservations += reservations.size();
            totalAmount = totalAmount.add(sumAmountKept(reservations));
        }

        report.appendTotal("total," + totalReservations + "," + totalAmount.toPlainString());
        return report.build();
    }

    private BigDecimal sumAmountKept(List<Reservation> reservations) {
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : reservations) {
            sum = sum.add(reservation.amountKept());
        }
        return sum;
    }
}
