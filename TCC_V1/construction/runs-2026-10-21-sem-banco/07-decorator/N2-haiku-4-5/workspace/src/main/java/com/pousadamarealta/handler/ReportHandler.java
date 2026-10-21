package com.pousadamarealta.handler;

import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

public class ReportHandler {
    private final ReportService reportService;

    public ReportHandler(ReportService reportService) {
        this.reportService = reportService;
    }

    public void occupancy(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "date parameter required"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        }
    }

    public void revenue(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "month parameter required"));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(yearMonth);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        }
    }
}
