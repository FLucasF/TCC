package com.pousada.api.controller;

import com.pousada.domain.service.ReportService;
import com.pousada.domain.validation.ValidationException;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String date = ctx.queryParam("date");
        validateDate(date);
        String csv = reportService.generateReport("occupancy", date);
        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String month = ctx.queryParam("month");
        validateMonth(month);
        String csv = reportService.generateReport("revenue", month);
        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }

    public void getTourismTaxReport(Context ctx) {
        String month = ctx.queryParam("month");
        validateMonth(month);
        String csv = reportService.generateReport("tourism-tax", month);
        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }

    public void getRatePlansReport(Context ctx) {
        String month = ctx.queryParam("month");
        validateMonth(month);
        String csv = reportService.generateReport("rate-plans", month);
        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }

    private void validateDate(String date) {
        if (date == null) {
            throw new ValidationException("INVALID_REQUEST", "Date parameter is required");
        }
        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ValidationException("INVALID_REQUEST", "Invalid date format. Expected: YYYY-MM-DD");
        }
    }

    private void validateMonth(String month) {
        if (month == null) {
            throw new ValidationException("INVALID_REQUEST", "Month parameter is required");
        }
        try {
            YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new ValidationException("INVALID_REQUEST", "Invalid month format. Expected: YYYY-MM");
        }
    }
}
