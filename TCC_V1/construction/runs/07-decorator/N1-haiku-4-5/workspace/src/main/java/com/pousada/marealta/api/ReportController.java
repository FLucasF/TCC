package com.pousada.marealta.api;

import com.pousada.marealta.service.ReportService;
import com.pousada.marealta.util.DateFormatter;
import io.javalin.Javalin;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void register(Javalin app) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isEmpty()) {
                    ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "date parameter is required"));
                    return;
                }

                LocalDate date = DateFormatter.parseISO(dateStr);
                String report = reportService.generateOccupancyReport(date);

                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "month parameter is required"));
                    return;
                }

                YearMonth yearMonth = YearMonth.parse(monthStr);
                String report = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());

                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
            }
        });
    }
}
