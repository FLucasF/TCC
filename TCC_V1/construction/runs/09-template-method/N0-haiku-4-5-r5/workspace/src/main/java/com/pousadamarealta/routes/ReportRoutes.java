package com.pousadamarealta.routes;

import io.javalin.http.Context;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.util.DateFormatter;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

public class ReportRoutes {
    private final ReportService reportService = new ReportService();

    public void occupancy(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date = DateFormatter.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void revenue(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(month);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void tourismTax(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateTourismTaxReport(month);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void ratePlans(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.generateRatePlansReport(month);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
