package com.pousadamarealta.api;

import com.google.gson.JsonObject;
import com.pousadamarealta.services.ReportService;
import io.javalin.Javalin;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportsController {
    public static void register(Javalin app, ReportService reportService) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isEmpty()) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "INVALID_REQUEST");
                    error.addProperty("message", "Missing date parameter");
                    ctx.status(400).json(error);
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr);
                String csv = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (DateTimeParseException e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "Invalid date format (use yyyy-MM-dd)");
                ctx.status(400).json(error);
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "INVALID_REQUEST");
                    error.addProperty("message", "Missing month parameter");
                    ctx.status(400).json(error);
                    return;
                }

                YearMonth yearMonth = YearMonth.parse(monthStr);
                String csv = reportService.generateRevenueReport(yearMonth);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (DateTimeParseException e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "Invalid month format (use yyyy-MM)");
                ctx.status(400).json(error);
            }
        });
    }
}
