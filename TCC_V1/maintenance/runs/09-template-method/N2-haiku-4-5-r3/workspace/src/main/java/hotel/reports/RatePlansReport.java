package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Rate plans report: revenue and count of reservations by plan for the month. */
public final class RatePlansReport extends Report<YearMonth> {

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    @Override
    protected String reportName() {
        return "rate-plans";
    }

    @Override
    protected String formatTitle(YearMonth month) {
        return titleLine(month);
    }

    @Override
    protected String formatHeader() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> formatLines(YearMonth month) {
        BigDecimal standardAmount = BigDecimal.ZERO.setScale(2);
        int standardCount = 0;
        BigDecimal nonRefundableAmount = BigDecimal.ZERO.setScale(2);
        int nonRefundableCount = 0;

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                if (reservation.ratePlan() == RatePlan.STANDARD) {
                    standardCount++;
                    standardAmount = standardAmount.add(reservation.amountKept());
                } else if (reservation.ratePlan() == RatePlan.NON_REFUNDABLE) {
                    nonRefundableCount++;
                    nonRefundableAmount = nonRefundableAmount.add(reservation.amountKept());
                }
            }
        }

        List<String> lines = new ArrayList<>();
        lines.add("STANDARD," + standardCount + "," + standardAmount.toPlainString());
        lines.add("NON_REFUNDABLE," + nonRefundableCount + "," + nonRefundableAmount.toPlainString());
        return lines;
    }

    @Override
    protected String formatTotal(YearMonth month) {
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                totalCount++;
                totalAmount = totalAmount.add(reservation.amountKept());
            }
        }

        return "total," + totalCount + "," + totalAmount.toPlainString();
    }
}
