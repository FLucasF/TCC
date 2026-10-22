package com.pousadamarealta.handler;

import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class ReportHandler {
    private final ReportService reportService;

    public ReportHandler(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String report = reportService.getOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String report = reportService.getRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }
}
