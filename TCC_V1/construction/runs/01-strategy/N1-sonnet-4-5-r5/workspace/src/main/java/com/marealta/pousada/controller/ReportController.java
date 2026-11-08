package com.marealta.pousada.controller;

import com.marealta.pousada.exception.InvalidRequestException;
import com.marealta.pousada.service.ReportService;
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
            throw new InvalidRequestException("Date parameter is required");
        }

        try {
            LocalDate date = LocalDate.parse(dateParam);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format. Expected: yyyy-MM-dd");
        }
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");

        if (monthParam == null || monthParam.isBlank()) {
            throw new InvalidRequestException("Month parameter is required");
        }

        try {
            YearMonth yearMonth = YearMonth.parse(monthParam);
            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format. Expected: yyyy-MM");
        }
    }
}
