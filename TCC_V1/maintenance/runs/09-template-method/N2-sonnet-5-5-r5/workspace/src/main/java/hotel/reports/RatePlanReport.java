package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** How many reservations and how much money each rate plan brought in a month, counted as the financial report does. */
public final class RatePlanReport extends Report<YearMonth> {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    @Override
    protected String name() {
        return "rate-plans";
    }

    @Override
    protected List<String> header() {
        return List.of("rate_plan", "reservations", "amount");
    }

    @Override
    protected Table table(YearMonth month) {
        List<Reservation> withRevenue = MonthlyRevenue.of(store, month);
        List<List<String>> lines = new ArrayList<>();
        int totalCount = 0;
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
            lines.add(List.of(plan.name(), String.valueOf(count), amount.toPlainString()));
            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }
        return new Table(lines, List.of("total", String.valueOf(totalCount), totalAmount.toPlainString()));
    }
}
