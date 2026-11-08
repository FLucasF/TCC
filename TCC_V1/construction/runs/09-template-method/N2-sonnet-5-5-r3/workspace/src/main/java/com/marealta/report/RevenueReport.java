package com.marealta.report;

import com.marealta.Money;
import com.marealta.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public final class RevenueReport extends Report {
    private final YearMonth month;
    private final List<Reservation> reservations;

    public RevenueReport(ReportData data, YearMonth month) {
        this.month = month;
        this.reservations = data.retainedIn(month);
    }

    @Override
    protected String name() { return "revenue"; }

    @Override
    protected String period() { return month.toString(); }

    @Override
    protected List<String> header() {
        return List.of("reservation", "room", "check_in", "check_out", "status", "amount");
    }

    @Override
    protected List<List<String>> rows() {
        return reservations.stream().map(r -> List.of(
                String.valueOf(r.id()),
                String.valueOf(r.roomNumber()),
                r.checkIn().toString(),
                r.checkOut().toString(),
                r.status().name(),
                Money.plain(r.retained()))).toList();
    }

    @Override
    protected List<String> total() {
        BigDecimal sum = reservations.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
        return List.of("total", Money.plain(sum));
    }
}
