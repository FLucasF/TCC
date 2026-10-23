package com.pousadamarealta.controller;

import com.pousadamarealta.service.OccupancyReportService;
import com.pousadamarealta.service.RevenueReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;

public class ReportController {
    private final OccupancyReportService occupancyReportService;
    private final RevenueReportService revenueReportService;

    public ReportController(OccupancyReportService occupancyReportService,
                          RevenueReportService revenueReportService) {
        this.occupancyReportService = occupancyReportService;
        this.revenueReportService = revenueReportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        LocalDate date = LocalDate.parse(dateStr);
        String csv = occupancyReportService.generateReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        YearMonth month = YearMonth.parse(monthStr);
        String csv = revenueReportService.generateReport(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }
}
