package com.pousadamarealta.api;

import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null || dateStr.isBlank()) {
            throw new InvalidRequestException("Missing required parameter: date");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format: " + dateStr);
        }

        String csv = reportService.generateOccupancyReport(date);

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null || monthStr.isBlank()) {
            throw new InvalidRequestException("Missing required parameter: month");
        }

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(monthStr);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format: " + monthStr);
        }

        String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }
}
