package br.marealta.api;

import br.marealta.service.ReportService;
import br.marealta.util.Formatters;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.util.Map;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Date parameter required"));
                return;
            }

            LocalDate date = Formatters.parseIsoDate(dateStr);
            String report = reportService.generateOccupancyReport(date);

            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Month parameter required"));
                return;
            }

            String[] parts = monthStr.split("-");
            if (parts.length != 2) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
                return;
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            if (month < 1 || month > 12) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month"));
                return;
            }

            String report = reportService.generateRevenueReport(year, month);

            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
        }
    }
}
