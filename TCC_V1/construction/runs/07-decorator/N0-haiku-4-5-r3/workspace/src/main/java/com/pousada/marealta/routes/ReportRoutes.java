package com.pousada.marealta.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.marealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReportRoutes {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isBlank()) {
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", "date parameter is required"));
                return;
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            var report = ReportService.getOccupancyReport(date);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(errorResponse("INTERNAL_ERROR", "Database error"));
        }
    }

    public static void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isBlank()) {
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", "month parameter is required"));
                return;
            }

            YearMonth month;
            try {
                month = YearMonth.parse(monthStr);
            } catch (DateTimeParseException e) {
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", "Invalid month format"));
                return;
            }

            var report = ReportService.getRevenueReport(month);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(errorResponse("INTERNAL_ERROR", "Database error"));
        }
    }

    private static Map<String, Object> errorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        response.put("message", message);
        return response;
    }
}
