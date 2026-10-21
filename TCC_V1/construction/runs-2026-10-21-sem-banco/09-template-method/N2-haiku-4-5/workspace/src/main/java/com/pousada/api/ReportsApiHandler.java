package com.pousada.api;

import com.pousada.service.ReportService;
import io.javalin.Javalin;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

public class ReportsApiHandler {
    public static void register(Javalin app, ReportService reportService) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateParam = ctx.queryParam("date");
                if (dateParam == null || dateParam.isEmpty()) {
                    ctx.status(400);
                    ctx.json(Map.of("error", "INVALID_REQUEST", "message", "date parameter is required"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateParam);
                String report = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null || monthParam.isEmpty()) {
                    ctx.status(400);
                    ctx.json(Map.of("error", "INVALID_REQUEST", "message", "month parameter is required"));
                    return;
                }

                YearMonth yearMonth = YearMonth.parse(monthParam);
                String report = reportService.generateRevenueReport(yearMonth);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
            }
        });

        app.get("/reports/tourism-tax", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null || monthParam.isEmpty()) {
                    ctx.status(400);
                    ctx.json(Map.of("error", "INVALID_REQUEST", "message", "month parameter is required"));
                    return;
                }

                YearMonth yearMonth = YearMonth.parse(monthParam);
                String report = reportService.generateTourismTaxReport(yearMonth);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
            }
        });

        app.get("/reports/rate-plans", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null || monthParam.isEmpty()) {
                    ctx.status(400);
                    ctx.json(Map.of("error", "INVALID_REQUEST", "message", "month parameter is required"));
                    return;
                }

                YearMonth yearMonth = YearMonth.parse(monthParam);
                String report = reportService.generateRatePlansReport(yearMonth);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
            }
        });
    }
}
