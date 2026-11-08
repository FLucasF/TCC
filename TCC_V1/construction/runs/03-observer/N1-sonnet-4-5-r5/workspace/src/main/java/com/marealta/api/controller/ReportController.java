package com.marealta.api.controller;

import com.marealta.api.error.ErrorHandler;
import com.marealta.domain.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void occupancy(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null || dateParam.isBlank()) {
            throw new ErrorHandler.InvalidRequestException("Date parameter is required");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateParam);
        } catch (DateTimeParseException e) {
            throw new ErrorHandler.InvalidRequestException("Invalid date format. Use yyyy-MM-dd");
        }

        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void revenue(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isBlank()) {
            throw new ErrorHandler.InvalidRequestException("Month parameter is required");
        }

        YearMonth month;
        try {
            month = YearMonth.parse(monthParam);
        } catch (DateTimeParseException e) {
            throw new ErrorHandler.InvalidRequestException("Invalid month format. Use yyyy-MM");
        }

        String csv = reportService.generateRevenueReport(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }
}
