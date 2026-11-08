package com.pousadamarealta.report;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RatePlansReport extends CsvReport {
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

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
        return month.format(MONTH_FORMATTER);
    }

    @Override
    protected String buildHeaderLine() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> buildDataLines() {
        Map<RatePlan, List<Reservation>> byPlan = reservations.stream()
            .collect(Collectors.groupingBy(Reservation::getRatePlan));

        List<String> lines = new ArrayList<>();

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> planReservations = byPlan.getOrDefault(plan, List.of());
            int count = planReservations.size();
            BigDecimal amount = planReservations.stream()
                .map(r -> r.getAmountPaid().subtract(
                    r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            lines.add(String.format("%s,%d,%s", plan.name(), count, formatDecimal(amount)));
        }

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        int totalCount = reservations.size();
        BigDecimal totalAmount = reservations.stream()
            .map(r -> r.getAmountPaid().subtract(
                r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO
            ))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format("total,%d,%s", totalCount, formatDecimal(totalAmount));
    }

    private String formatDecimal(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
