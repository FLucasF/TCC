package com.pousadamarealta.service.report;

import com.pousadamarealta.domain.RatePlanType;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RatePlansReportGenerator implements ReportGenerator {
    @Override
    public String generate(List<Reservation> reservations, List<Room> rooms, String monthStr) {
        YearMonth month = YearMonth.parse(monthStr);

        List<Reservation> monthReservations = reservations.stream()
            .filter(r -> {
                YearMonth checkInMonth = YearMonth.from(r.getCheckIn());
                return checkInMonth.equals(month);
            })
            .filter(r -> r.getNetRevenue().compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.toList());

        Map<RatePlanType, List<Reservation>> byPlan = monthReservations.stream()
            .collect(Collectors.groupingBy(Reservation::getRatePlanType));

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(monthStr).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        BigDecimal grandTotal = BigDecimal.ZERO;
        int totalCount = 0;

        for (RatePlanType planType : RatePlanType.values()) {
            List<Reservation> planReservations = byPlan.getOrDefault(planType, List.of());
            int count = planReservations.size();
            BigDecimal amount = planReservations.stream()
                .map(Reservation::getNetRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalCount += count;
            grandTotal = grandTotal.add(amount);

            csv.append(planType).append(",")
               .append(count).append(",")
               .append(formatMoney(amount)).append("\n");
        }

        csv.append("total,").append(totalCount).append(",").append(formatMoney(grandTotal));

        return csv.toString();
    }

    private String formatMoney(BigDecimal amount) {
        return amount.toPlainString();
    }
}
