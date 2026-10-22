package com.pousadamarealta.service.report;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RatePlansReport extends Report {
    private final YearMonth month;
    private final List<Reservation> reservations;

    public RatePlansReport(YearMonth month, List<Reservation> reservations) {
        this.month = month;
        this.reservations = reservations;
    }

    @Override
    protected String getReportName() {
        return "rate-plans";
    }

    @Override
    protected String getPeriod() {
        return month.toString();
    }

    @Override
    protected String buildHeader() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> buildDataLines() {
        Map<RatePlan, List<Reservation>> byPlan = reservations.stream()
                .filter(r -> r.getNetRevenue().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(Reservation::getRatePlan));

        List<String> lines = new ArrayList<>();

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> planReservations = byPlan.getOrDefault(plan, List.of());
            long count = planReservations.size();
            BigDecimal amount = planReservations.stream()
                    .map(Reservation::getNetRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            lines.add(joinCsvFields(plan.name(), count, formatCsvMoney(amount)));
        }

        return lines;
    }

    @Override
    protected String buildTotal() {
        long totalCount = reservations.stream()
                .filter(r -> r.getNetRevenue().compareTo(BigDecimal.ZERO) > 0)
                .count();

        BigDecimal totalAmount = reservations.stream()
                .map(Reservation::getNetRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return joinCsvFields("total", totalCount, formatCsvMoney(totalAmount));
    }
}
