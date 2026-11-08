package com.pousadamarealta.api;

import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null || dateParam.isBlank()) {
                ctx.status(400).json(ApiResponse.error("INVALID_REQUEST", "date parameter is required"));
                return;
            }

            LocalDate date = LocalDate.parse(dateParam, DateTimeFormatter.ISO_LOCAL_DATE);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(400).json(ApiResponse.error("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isBlank()) {
                ctx.status(400).json(ApiResponse.error("INVALID_REQUEST", "month parameter is required"));
                return;
            }

            YearMonth month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
            String csv = reportService.generateRevenueReport(month);

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(400).json(ApiResponse.error("INVALID_REQUEST", "Invalid month format"));
        }
    }

    public void getTourismTaxReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isBlank()) {
                ctx.status(400).json(ApiResponse.error("INVALID_REQUEST", "month parameter is required"));
                return;
            }

            YearMonth month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
            String csv = reportService.generateTourismTaxReport(month);

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(400).json(ApiResponse.error("INVALID_REQUEST", "Invalid month format"));
        }
    }

    public void getRatePlanReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isBlank()) {
                ctx.status(400).json(ApiResponse.error("INVALID_REQUEST", "month parameter is required"));
                return;
            }

            YearMonth month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
            String csv = reportService.generateRatePlanReport(month);

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(400).json(ApiResponse.error("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
