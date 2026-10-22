package com.pousadamarealta.report;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public abstract class ReportGenerator {
    protected static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    protected static final DecimalFormat DECIMAL_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMAT = new DecimalFormat("0.00", symbols);
    }

    public final String generate() {
        StringBuilder csv = new StringBuilder();
        csv.append(buildTitleLine()).append("\n");
        csv.append(buildHeaderLine()).append("\n");
        csv.append(buildDataLines());
        csv.append(buildTotalLine()).append("\n");
        return csv.toString();
    }

    protected abstract String getReportName();
    protected abstract String getPeriod();
    protected abstract String buildHeaderLine();
    protected abstract String buildDataLines();
    protected abstract String buildTotalLine();

    private String buildTitleLine() {
        return "Pousada Maré Alta," + getReportName() + "," + getPeriod();
    }

    protected String formatDecimal(BigDecimal value) {
        return DECIMAL_FORMAT.format(value);
    }

    protected String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    protected String formatMonth(YearMonth month) {
        return month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}
