package com.pousadamarealta.api.controller;

import com.pousadamarealta.domain.service.ReportService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) throws SQLException {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null) {
            throw new IllegalArgumentException("Missing date parameter");
        }

        LocalDate date = LocalDate.parse(dateParam);
        String csv = reportService.generateOccupancyReport(date);

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }

    public void getRevenueReport(Context ctx) throws SQLException {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            throw new IllegalArgumentException("Missing month parameter");
        }

        YearMonth month = YearMonth.parse(monthParam);
        String csv = reportService.generateRevenueReport(month);

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }
}
