package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Which rate plan brings more money: reservations and money kept per plan, over the revenue's reservations (CSV). */
public final class RatePlanReport extends MonthlyReport {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "rate-plans";
    }

    @Override
    protected Table table(YearMonth month) {
        List<Reservation> reservations = MonthReservations.withRevenue(store, month);
        List<String> rows = new ArrayList<>();
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = reservations.stream().filter(r -> r.ratePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream().map(Reservation::amountKept)
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
            rows.add(line(plan, ofPlan.size(), amount.toPlainString()));
            totalCount += ofPlan.size();
            totalAmount = totalAmount.add(amount);
        }
        return new Table("rate_plan,reservations,amount", rows,
                line("total", totalCount, totalAmount.toPlainString()));
    }
}
