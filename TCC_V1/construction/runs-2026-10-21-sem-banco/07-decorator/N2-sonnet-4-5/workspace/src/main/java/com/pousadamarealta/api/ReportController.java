package com.pousadamarealta.api;

import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                error(ctx, 400, "INVALID_REQUEST", "Missing date parameter");
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid date format");
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                error(ctx, 400, "INVALID_REQUEST", "Missing month parameter");
                return;
            }

            String[] parts = monthStr.split("-");
            if (parts.length != 2) {
                error(ctx, 400, "INVALID_REQUEST", "Invalid month format");
                return;
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            if (month < 1 || month > 12) {
                error(ctx, 400, "INVALID_REQUEST", "Invalid month");
                return;
            }

            String csv = reportService.generateRevenueReport(year, month);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid month format");
        }
    }

    private void error(Context ctx, int status, String code, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", code);
        error.put("message", message);
        ctx.status(status).json(error);
    }
}
