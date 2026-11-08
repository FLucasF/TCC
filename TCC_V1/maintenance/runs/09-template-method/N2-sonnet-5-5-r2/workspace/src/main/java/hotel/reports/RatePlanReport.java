package hotel.reports;

import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Which rate plan brings more money, for the management (CSV): one line per plan with the reservations the
 * revenue report counts for the month.
 */
public final class RatePlanReport extends Report<YearMonth, RatePlanReport.PlanLine> {

    record PlanLine(RatePlan plan, int reservations, BigDecimal amount) {
    }

    private final RevenueReport revenue;

    public RatePlanReport(RevenueReport revenue) {
        this.revenue = revenue;
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
    protected List<PlanLine> items(YearMonth month) {
        List<Reservation> counted = revenue.items(month);
        List<PlanLine> lines = new ArrayList<>();
        for (RatePlan plan : RatePlan.values()) {
            int count = 0;
            BigDecimal amount = BigDecimal.ZERO.setScale(2);
            for (Reservation reservation : counted) {
                if (reservation.ratePlan() == plan) {
                    count++;
                    amount = amount.add(reservation.amountKept());
                }
            }
            lines.add(new PlanLine(plan, count, amount));
        }
        return lines;
    }

    @Override
    protected String line(PlanLine line) {
        return line.plan() + "," + line.reservations() + "," + line.amount().toPlainString();
    }

    @Override
    protected String totalLine(List<PlanLine> lines) {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
        for (PlanLine line : lines) {
            count += line.reservations();
            amount = amount.add(line.amount());
        }
        return "total," + count + "," + amount.toPlainString();
    }
}
