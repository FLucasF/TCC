package com.marealta.report;

import com.marealta.Money;
import com.marealta.RatePlan;
import com.marealta.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

public final class RatePlansReport extends Report {
    private final YearMonth month;
    private final List<Reservation> reservations;

    public RatePlansReport(ReportData data, YearMonth month) {
        this.month = month;
        this.reservations = data.retainedIn(month);
    }

    @Override
    protected String name() { return "rate-plans"; }

    @Override
    protected String period() { return month.toString(); }

    @Override
    protected List<String> header() { return List.of("rate_plan", "reservations", "amount"); }

    @Override
    protected List<List<String>> rows() {
        return Arrays.stream(RatePlan.values()).map(plan -> {
            List<Reservation> ofPlan = reservations.stream().filter(r -> r.ratePlan() == plan).toList();
            return List.of(plan.name(), String.valueOf(ofPlan.size()), Money.plain(sum(ofPlan)));
        }).toList();
    }

    @Override
    protected List<String> total() {
        return List.of("total", String.valueOf(reservations.size()), Money.plain(sum(reservations)));
    }

    private static BigDecimal sum(List<Reservation> list) {
        return list.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
    }
}
