package com.pousada.api.controller;

import com.pousada.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null || dateParam.isBlank()) {
            throw new IllegalArgumentException("Date parameter is required");
        }

        LocalDate date = LocalDate.parse(dateParam);
        String csv = reportService.generateOccupancyReport(date);

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isBlank()) {
            throw new IllegalArgumentException("Month parameter is required");
        }

        YearMonth month = YearMonth.parse(monthParam);
        String csv = reportService.generateRevenueReport(month);

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }
}
