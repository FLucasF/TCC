package com.pousada;

import com.pousada.reports.OccupancyReport;
import com.pousada.reports.RatePlansReport;
import com.pousada.reports.RevenueReport;
import com.pousada.reports.TourismTaxReport;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportsHandler {
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    public void occupancy(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null || dateParam.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "date parameter is required"));
                return;
            }

            LocalDate date = LocalDate.parse(dateParam, dateFormatter);
            String report = new OccupancyReport().generate(date);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void revenue(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "month parameter is required"));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthParam);
            LocalDate date = yearMonth.atDay(1);
            String report = new RevenueReport().generate(date);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void tourismTax(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "month parameter is required"));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthParam);
            LocalDate date = yearMonth.atDay(1);
            String report = new TourismTaxReport().generate(date);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void ratePlans(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "month parameter is required"));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthParam);
            LocalDate date = yearMonth.atDay(1);
            String report = new RatePlansReport().generate(date);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    static class ErrorResponse {
        String error;
        String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
