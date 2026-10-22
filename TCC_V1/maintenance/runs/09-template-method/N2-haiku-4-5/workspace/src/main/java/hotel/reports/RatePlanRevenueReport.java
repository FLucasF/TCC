package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Rate plan revenue report: shows revenue by rate plan (STANDARD and NON_REFUNDABLE) for paid reservations starting in the month. */
public final class RatePlanRevenueReport extends Report {

    private final ReservationStore store;
    private YearMonth month;

    public RatePlanRevenueReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        this.month = month;
        return build();
    }

    @Override
    protected String reportName() {
        return "rate-plans";
    }

    @Override
    protected String period() {
        return month.toString();
    }

    @Override
    protected String headers() {
        return "rate_plan,reservations,amount\n";
    }

    @Override
    protected String rows() {
        StringBuilder csv = new StringBuilder();
        for (RatePlan plan : RatePlan.values()) {
            long count = 0;
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : relevantReservations()) {
                if (reservation.ratePlan() == plan) {
                    count++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            csv.append(plan).append(',')
                    .append(count).append(',')
                    .append(amount.toPlainString()).append('\n');
        }
        return csv.toString();
    }

    @Override
    protected String total() {
        List<Reservation> relevant = relevantReservations();
        long totalCount = relevant.size();
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : relevant) {
            totalAmount = totalAmount.add(reservation.amountKept());
        }
        return "total," + totalCount + "," + totalAmount.toPlainString() + "\n";
    }

    private List<Reservation> relevantReservations() {
        List<Reservation> relevant = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean isPaid = reservation.isPaid();
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (isPaid && startsInMonth) {
                relevant.add(reservation);
            }
        }
        return relevant;
    }
}
