package com.pousadamareal.controller;

import com.pousadamareal.api.ErrorResponse;
import com.pousadamareal.service.ReportService;
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

    public void occupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateParam, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void revenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
            return;
        }

        try {
            YearMonth yearMonth = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
