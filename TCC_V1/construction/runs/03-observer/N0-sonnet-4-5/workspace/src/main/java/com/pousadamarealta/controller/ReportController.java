package com.pousadamarealta.controller;

import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancy(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", "Missing date parameter"
            ));
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);
        } catch (Exception e) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", "Invalid date format"
            ));
        }
    }

    public void getRevenue(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", "Missing month parameter"
            ));
            return;
        }

        try {
            YearMonth yearMonth = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);
        } catch (Exception e) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", "Invalid month format"
            ));
        }
    }
}
