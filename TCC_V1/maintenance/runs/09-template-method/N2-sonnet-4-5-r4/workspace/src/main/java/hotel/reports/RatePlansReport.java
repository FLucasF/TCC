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
 * The revenue by rate plan, for the managers to see which plan brings more money (CSV).
 * Counts the same reservations the revenue report counts.
 */
public final class RatePlansReport extends CsvReport {

    private final ReservationStore store;
    private final Map<RatePlan, List<Reservation>> byPlan = new EnumMap<>(RatePlan.class);
    private int totalReservations;
    private BigDecimal totalAmount;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        byPlan.clear();
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
        return assemble(month.toString());
    }

    @Override
    protected String reportName() {
        return "rate-plans";
    }

    @Override
    protected String headers() {
        return "rate_plan,reservations,amount\n";
    }

    @Override
    protected String dataLines() {
        StringBuilder lines = new StringBuilder();
        totalReservations = 0;
        totalAmount = BigDecimal.ZERO.setScale(2);

        lines.append(planLine(RatePlan.STANDARD));
        lines.append(planLine(RatePlan.NON_REFUNDABLE));

        return lines.toString();
    }

    private String planLine(RatePlan plan) {
        List<Reservation> reservations = byPlan.get(plan);
        int count = reservations.size();
        BigDecimal amount = reservations.stream()
                .map(Reservation::amountKept)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalReservations += count;
        totalAmount = totalAmount.add(amount);

        return plan.name() + ',' + count + ',' + amount.toPlainString() + '\n';
    }

    @Override
    protected String totalLine() {
        return "total," + totalReservations + ',' + totalAmount.toPlainString() + '\n';
    }
}
