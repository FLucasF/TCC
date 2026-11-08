package com.pousadamarealta.report;

import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TourismTaxReport extends CsvReport {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

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
        return month.format(MONTH_FORMATTER);
    }

    @Override
    protected String buildHeaderLine() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> buildDataLines() {
        return reservations.stream()
            .map(r -> {
                BigDecimal tax = calculateTax(r.getGuests(), r.getNights());
                return String.format("%d,%s,%s,%s,%d,%d,%s",
                    r.getId(),
                    r.getRoomNumber(),
                    r.getCheckIn().format(DATE_FORMATTER),
                    r.getCheckOut().format(DATE_FORMATTER),
                    r.getGuests(),
                    r.getNights(),
                    formatDecimal(tax));
            })
            .collect(Collectors.toList());
    }

    @Override
    protected String buildTotalLine() {
        BigDecimal total = reservations.stream()
            .map(r -> calculateTax(r.getGuests(), r.getNights()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return "total," + formatDecimal(total);
    }

    private BigDecimal calculateTax(int guests, int nights) {
        return TAX_PER_GUEST_PER_NIGHT.multiply(BigDecimal.valueOf(guests * nights));
    }

    private String formatDecimal(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
