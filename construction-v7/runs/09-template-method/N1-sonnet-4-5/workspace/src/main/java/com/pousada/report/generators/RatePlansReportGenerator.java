package com.pousada.report.generators;

import com.pousada.domain.model.RatePlan;
import com.pousada.domain.model.Reservation;
import com.pousada.infrastructure.repository.RoomRepository;
import com.pousada.report.ReportGenerator;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RatePlansReportGenerator implements ReportGenerator {

    @Override
    public String getName() {
        return "rate-plans";
    }

    @Override
    public String generate(String period, List<Reservation> reservations, RoomRepository roomRepository) {
        YearMonth month = YearMonth.parse(period);

        List<Reservation> monthReservations = reservations.stream()
            .filter(r -> {
                YearMonth checkInMonth = YearMonth.from(r.getCheckIn());
                return checkInMonth.equals(month);
            })
            .filter(r -> r.getNetAmount().compareTo(BigDecimal.ZERO) > 0)
            .toList();

        Map<RatePlan, List<Reservation>> byPlan = monthReservations.stream()
            .collect(Collectors.groupingBy(Reservation::getRatePlan));

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,").append(getName()).append(",").append(period).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        int totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> planReservations = byPlan.getOrDefault(plan, List.of());
            int count = planReservations.size();
            BigDecimal amount = planReservations.stream()
                .map(Reservation::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            csv.append(plan).append(",")
               .append(count).append(",")
               .append(formatDecimal(amount)).append("\n");

            totalReservations += count;
            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,").append(totalReservations).append(",").append(formatDecimal(totalAmount));

        return csv.toString();
    }

    private String formatDecimal(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
