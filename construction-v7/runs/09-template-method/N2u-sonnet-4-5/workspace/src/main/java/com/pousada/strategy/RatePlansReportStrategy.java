package com.pousada.strategy;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.Room;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RatePlansReportStrategy implements ReportStrategy {
    @Override
    public String generate(List<Reservation> reservations, List<Room> rooms, Object parameter) {
        YearMonth month = (YearMonth) parameter;
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month.format(monthFormatter)).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        Map<RatePlan, PlanStats> stats = new HashMap<>();
        stats.put(RatePlan.STANDARD, new PlanStats());
        stats.put(RatePlan.NON_REFUNDABLE, new PlanStats());

        reservations.stream()
                .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
                .forEach(r -> {
                    BigDecimal netRevenue = getNetRevenue(r);
                    if (netRevenue.compareTo(BigDecimal.ZERO) > 0) {
                        PlanStats planStats = stats.get(r.getRatePlan());
                        planStats.count++;
                        planStats.amount = planStats.amount.add(netRevenue);
                    }
                });

        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (RatePlan plan : RatePlan.values()) {
            PlanStats planStats = stats.get(plan);
            csv.append(plan).append(",");
            csv.append(planStats.count).append(",");
            csv.append(formatCsvNumber(planStats.amount)).append("\n");

            totalCount += planStats.count;
            totalAmount = totalAmount.add(planStats.amount);
        }

        csv.append("total,").append(totalCount).append(",").append(formatCsvNumber(totalAmount));

        return csv.toString();
    }

    @Override
    public String getReportName() {
        return "rate-plans";
    }

    private BigDecimal getNetRevenue(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refunded = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private String formatCsvNumber(BigDecimal number) {
        return number.setScale(2).toString().replace(",", "");
    }

    private static class PlanStats {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO;
    }
}
