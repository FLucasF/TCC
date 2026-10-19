package com.pousadamareal.api;

import com.pousadamareal.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReportController {
    private final ReportService reportService;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400);
                Map<String, String> error = new HashMap<>();
                error.put("error", "INVALID_REQUEST");
                error.put("message", "Missing date parameter");
                ctx.json(error);
                return;
            }

            LocalDate date = LocalDate.parse(dateStr, ISO_DATE);
            String report = reportService.generateOccupancyReport(date);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400);
            Map<String, String> error = new HashMap<>();
            error.put("error", "INVALID_REQUEST");
            error.put("message", "Invalid date format");
            ctx.json(error);
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400);
                Map<String, String> error = new HashMap<>();
                error.put("error", "INVALID_REQUEST");
                error.put("message", "Missing month parameter");
                ctx.json(error);
                return;
            }

            YearMonth month = YearMonth.parse(monthStr, ISO_MONTH);
            String report = reportService.generateRevenueReport(month);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400);
            Map<String, String> error = new HashMap<>();
            error.put("error", "INVALID_REQUEST");
            error.put("message", "Invalid month format");
            ctx.json(error);
        }
    }
}
