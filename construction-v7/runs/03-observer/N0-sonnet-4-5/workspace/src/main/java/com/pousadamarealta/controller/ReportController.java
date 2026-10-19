package com.pousadamarealta.controller;

import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null) {
            throw new IllegalArgumentException("Missing required parameter: date");
        }

        LocalDate date = LocalDate.parse(dateStr);
        String csv = reportService.generateOccupancyReport(date);

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new IllegalArgumentException("Missing required parameter: month");
        }

        YearMonth month = YearMonth.parse(monthStr);
        String csv = reportService.generateRevenueReport(month);

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }
}
