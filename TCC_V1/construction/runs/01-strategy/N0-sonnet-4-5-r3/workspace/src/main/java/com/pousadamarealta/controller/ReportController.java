package com.pousadamarealta.controller;

import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null || dateStr.isEmpty()) {
            throw new ApiException(400, "INVALID_REQUEST", "Date parameter is required");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid date format. Expected: yyyy-MM-dd");
        }

        String csv = reportService.getOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null || monthStr.isEmpty()) {
            throw new ApiException(400, "INVALID_REQUEST", "Month parameter is required");
        }

        YearMonth month;
        try {
            month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid month format. Expected: yyyy-MM");
        }

        String csv = reportService.getRevenueReport(month);
        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }
}
