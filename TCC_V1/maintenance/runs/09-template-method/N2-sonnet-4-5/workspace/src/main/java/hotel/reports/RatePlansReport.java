package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Revenue grouped by rate plan for reservations starting in a given month.
 */
public final class RatePlansReport extends Report<RatePlansReport.RatePlanRow> {

    record RatePlanRow(RatePlan ratePlan, int reservations, BigDecimal amount) {}

    private final ReservationStore store;

    public RatePlansReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        return generateCsv(month);
    }

    @Override
    protected String reportName() {
        return "rate-plans";
    }

    @Override
    protected List<RatePlanRow> collectData(Object period) {
        YearMonth month = (YearMonth) period;

        // Count reservations and sum amounts for each rate plan
        int standardCount = 0;
        BigDecimal standardAmount = BigDecimal.ZERO.setScale(2);
        int nonRefundableCount = 0;
        BigDecimal nonRefundableAmount = BigDecimal.ZERO.setScale(2);

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

        // Always include both plans, even if one has no reservations
        List<RatePlanRow> rows = new ArrayList<>();
        rows.add(new RatePlanRow(RatePlan.STANDARD, standardCount, standardAmount));
        rows.add(new RatePlanRow(RatePlan.NON_REFUNDABLE, nonRefundableCount, nonRefundableAmount));
        return rows;
    }

    @Override
    protected String headerLine() {
        return "rate_plan,reservations,amount\n";
    }

    @Override
    protected String dataLine(RatePlanRow row) {
        return row.ratePlan + "," + row.reservations + "," + row.amount.toPlainString() + "\n";
    }

    @Override
    protected String totalLine(List<RatePlanRow> data) {
        int totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        for (RatePlanRow row : data) {
            totalReservations += row.reservations;
            totalAmount = totalAmount.add(row.amount);
        }
        return "total," + totalReservations + "," + totalAmount.toPlainString() + "\n";
    }
}
