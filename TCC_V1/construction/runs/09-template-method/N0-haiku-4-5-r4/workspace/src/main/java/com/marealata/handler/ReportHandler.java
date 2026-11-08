package com.marealata.handler;

import com.marealata.service.ReportService;
import io.javalin.Javalin;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

public class ReportHandler {
    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }

    public static void register(Javalin app) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "date parameter is required"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr);
                String csv = ReportService.generateOccupancyReport(date);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "month parameter is required"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthStr);
                String csv = ReportService.generateRevenueReport(month);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.get("/reports/tourism-tax", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "month parameter is required"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthStr);
                String csv = ReportService.generateTourismTaxReport(month);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });

        app.get("/reports/rate-plans", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "month parameter is required"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthStr);
                String csv = ReportService.generateRatePlansReport(month);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });
    }
}
