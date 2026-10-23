package com.pousada.api;

import com.pousada.service.InvalidRequestException;
import com.pousada.service.ReportService;
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
        if (dateStr == null) {
            throw new InvalidRequestException("Date parameter is required");
        }

        try {
            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format: " + dateStr);
        }
    }

    public void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new InvalidRequestException("Month parameter is required");
        }

        try {
            YearMonth yearMonth = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format: " + monthStr);
        }
    }
}
