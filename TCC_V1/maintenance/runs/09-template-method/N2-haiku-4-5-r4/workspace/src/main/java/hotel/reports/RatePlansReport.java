package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Revenue per rate plan, for management (CSV). */
public final class RatePlansReport extends Report {

    private final ReservationStore store;
    private final YearMonth month;
    private int totalReservations;
    private BigDecimal totalAmount;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
        this.month = null;
    }

    private RatePlansReport(ReservationStore store, YearMonth month) {
        this.store = store;
        this.month = month;
    }

    public String generate(YearMonth month) {
        return new RatePlansReport(store, month).generate();
    }

    @Override
    protected String title() {
        return POUSADA_NAME + ",rate-plans," + month;
    }

    @Override
    protected String header() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected void data(List<String> lines) {
        totalReservations = 0;
        totalAmount = BigDecimal.ZERO.setScale(2);

        for (RatePlan plan : RatePlan.values()) {
            int count = 0;
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : store.all()) {
                boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
                boolean keptMoney = reservation.amountKept().signum() > 0;
                if (reservation.ratePlan() == plan && startsInMonth && keptMoney) {
                    count++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            lines.add(plan + "," + count + "," + amount.toPlainString());
            totalReservations += count;
            totalAmount = totalAmount.add(amount);
        }
    }

    @Override
    protected String total() {
        return "total," + totalReservations + "," + totalAmount.toPlainString();
    }
}
