package com.pousada.controller;

import com.pousada.exception.BusinessException;
import com.pousada.service.ReportService;
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
        String dateParam = ctx.queryParam("date");

        if (dateParam == null || dateParam.isEmpty()) {
            throw new BusinessException("INVALID_REQUEST", "Date parameter is required", 400);
        }

        try {
            LocalDate date = LocalDate.parse(dateParam);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            throw new BusinessException("INVALID_REQUEST", "Invalid date format. Use yyyy-MM-dd", 400);
        }
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");

        if (monthParam == null || monthParam.isEmpty()) {
            throw new BusinessException("INVALID_REQUEST", "Month parameter is required", 400);
        }

        try {
            YearMonth month = YearMonth.parse(monthParam);
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            throw new BusinessException("INVALID_REQUEST", "Invalid month format. Use yyyy-MM", 400);
        }
    }
}
