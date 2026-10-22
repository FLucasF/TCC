package com.pousadamarealta.api;

import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ReportsHandler {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    public static void getOccupancyReport(Context ctx) throws Exception {
        String dateStr = ctx.queryParam("date");

        if (dateStr == null || dateStr.isEmpty()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Missing date parameter"));
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            String report = ReportService.getInstance().generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
        }
    }

    public static void getRevenueReport(Context ctx) throws Exception {
        String monthStr = ctx.queryParam("month");

        if (monthStr == null || monthStr.isEmpty()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Missing month parameter"));
            return;
        }

        try {
            YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
            String report = ReportService.getInstance().generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
        }
    }
}
