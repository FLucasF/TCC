package hotel.reports;

import hotel.reservations.PriceCalculator;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** How many reservations, and how much money kept, each rate plan brought in a month (CSV). */
public final class RatePlanReport extends CsvReport<YearMonth> {

    private final ReservationStore store;

    public RatePlanReport(ReservationStore store) {
        this.store = store;
    }

    @Override
    protected String name() {
        return "rate-plans";
    }

    @Override
    protected String header() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected Body body(YearMonth month) {
        List<Reservation> counted = store.keptMoneyStartingIn(month);
        List<String> rows = new ArrayList<>();
        int totalCount = 0;
        BigDecimal totalAmount = PriceCalculator.cents(BigDecimal.ZERO);
        for (RatePlan plan : RatePlan.values()) {
            int count = 0;
            BigDecimal amount = PriceCalculator.cents(BigDecimal.ZERO);
            for (Reservation reservation : counted) {
                if (reservation.ratePlan() == plan) {
                    count++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            rows.add(line(plan, count, amount.toPlainString()));
            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }
        return new Body(rows, "total," + totalCount + "," + totalAmount.toPlainString());
    }
}
