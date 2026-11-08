package com.pousadamarealta.report;

import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class RevenueReport extends CsvReport {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

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
        return month.format(MONTH_FORMATTER);
    }

    @Override
    protected String buildHeaderLine() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> buildDataLines() {
        return reservations.stream()
            .map(r -> {
                BigDecimal netAmount = r.getAmountPaid().subtract(
                    r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO
                );
                return String.format("%d,%s,%s,%s,%s,%s",
                    r.getId(),
                    r.getRoomNumber(),
                    r.getCheckIn().format(DATE_FORMATTER),
                    r.getCheckOut().format(DATE_FORMATTER),
                    r.getStatus().name(),
                    formatDecimal(netAmount));
            })
            .collect(Collectors.toList());
    }

    @Override
    protected String buildTotalLine() {
        BigDecimal total = reservations.stream()
            .map(r -> r.getAmountPaid().subtract(
                r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO
            ))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return "total," + formatDecimal(total);
    }

    private String formatDecimal(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
