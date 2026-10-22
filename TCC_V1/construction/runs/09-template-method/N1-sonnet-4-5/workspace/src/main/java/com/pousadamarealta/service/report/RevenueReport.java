package com.pousadamarealta.service.report;

import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

public class RevenueReport extends Report {
    private final YearMonth month;
    private final List<Reservation> reservations;

    public RevenueReport(YearMonth month, List<Reservation> reservations) {
        this.month = month;
        this.reservations = reservations;
    }

    @Override
    protected String getReportName() {
        return "revenue";
    }

    @Override
    protected String getPeriod() {
        return month.toString();
    }

    @Override
    protected String buildHeader() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> buildDataLines() {
        return reservations.stream()
                .filter(r -> r.getNetRevenue().compareTo(BigDecimal.ZERO) > 0)
                .map(this::buildReservationLine)
                .collect(Collectors.toList());
    }

    private String buildReservationLine(Reservation reservation) {
        return joinCsvFields(
                reservation.getId(),
                reservation.getRoomNumber(),
                formatCsvDate(reservation.getCheckIn()),
                formatCsvDate(reservation.getCheckOut()),
                reservation.getStatus().name(),
                formatCsvMoney(reservation.getNetRevenue())
        );
    }

    @Override
    protected String buildTotal() {
        BigDecimal total = reservations.stream()
                .map(Reservation::getNetRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return String.format("total,%s", formatCsvMoney(total));
    }
}
