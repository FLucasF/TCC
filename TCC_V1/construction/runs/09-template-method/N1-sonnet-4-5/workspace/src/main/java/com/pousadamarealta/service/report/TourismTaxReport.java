package com.pousadamarealta.service.report;

import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

public class TourismTaxReport extends Report {
    private final YearMonth month;
    private final List<Reservation> reservations;

    public TourismTaxReport(YearMonth month, List<Reservation> reservations) {
        this.month = month;
        this.reservations = reservations;
    }

    @Override
    protected String getReportName() {
        return "tourism-tax";
    }

    @Override
    protected String getPeriod() {
        return month.toString();
    }

    @Override
    protected String buildHeader() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> buildDataLines() {
        return reservations.stream()
                .filter(Reservation::isPaid)
                .map(this::buildReservationLine)
                .collect(Collectors.toList());
    }

    private String buildReservationLine(Reservation reservation) {
        return joinCsvFields(
                reservation.getId(),
                reservation.getRoomNumber(),
                formatCsvDate(reservation.getCheckIn()),
                formatCsvDate(reservation.getCheckOut()),
                reservation.getGuests(),
                reservation.getNights(),
                formatCsvMoney(reservation.calculateTourismTax())
        );
    }

    @Override
    protected String buildTotal() {
        BigDecimal total = reservations.stream()
                .filter(Reservation::isPaid)
                .map(Reservation::calculateTourismTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return String.format("total,%s", formatCsvMoney(total));
    }
}
