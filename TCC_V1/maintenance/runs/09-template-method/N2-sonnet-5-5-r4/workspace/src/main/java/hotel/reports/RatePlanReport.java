package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Which rate plan brings the most money, for the management (CSV): one line per plan, counting the same
 * reservations as the {@link RevenueReport}.
 */
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
    protected String header() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected Content content(YearMonth month) {
        List<String> rows = new ArrayList<>();
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (RatePlan plan : RatePlan.values()) {
            int count = 0;
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : store.all()) {
                boolean keptMoney = reservation.amountKept().signum() > 0;
                boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
                if (reservation.ratePlan() == plan && keptMoney && startsInMonth) {
                    count++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            rows.add(plan + "," + count + "," + amount.toPlainString());
            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }
        return new Content(rows, "total," + totalCount + "," + totalAmount.toPlainString());
    }
}
