package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.error.ErrorCode;
import com.pousadamarealta.api.error.ErrorResponse;
import com.pousadamarealta.domain.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");

        if (dateParam == null || dateParam.isEmpty()) {
            ctx.status(400).json(new ErrorResponse(
                ErrorCode.INVALID_REQUEST.name(),
                "Missing date parameter"
            ));
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateParam, DATE_FORMATTER);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse(
                ErrorCode.INVALID_REQUEST.name(),
                "Invalid date format"
            ));
        }
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");

        if (monthParam == null || monthParam.isEmpty()) {
            ctx.status(400).json(new ErrorResponse(
                ErrorCode.INVALID_REQUEST.name(),
                "Missing month parameter"
            ));
            return;
        }

        try {
            YearMonth yearMonth = YearMonth.parse(monthParam, MONTH_FORMATTER);
            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse(
                ErrorCode.INVALID_REQUEST.name(),
                "Invalid month format"
            ));
        }
    }
}
