package br.com.marealta.controller;

import br.com.marealta.exception.InvalidRequestException;
import br.com.marealta.service.ReportService;
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
        String dateParam = ctx.queryParam("date");

        if (dateParam == null || dateParam.isBlank()) {
            throw new InvalidRequestException("Missing required parameter: date");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateParam);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format: " + dateParam);
        }

        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");

        if (monthParam == null || monthParam.isBlank()) {
            throw new InvalidRequestException("Missing required parameter: month");
        }

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(monthParam);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format: " + monthParam);
        }

        String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }
}
