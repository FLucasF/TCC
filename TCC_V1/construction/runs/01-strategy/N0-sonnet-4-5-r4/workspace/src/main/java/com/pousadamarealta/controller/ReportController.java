package com.pousadamarealta.controller;

import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null || dateParam.isEmpty()) {
            throw new ApiException(400, "INVALID_REQUEST", "Date parameter is required");
        }

        LocalDate date = parseDate(dateParam);
        String csv = reportService.generateOccupancyReport(date);

        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isEmpty()) {
            throw new ApiException(400, "INVALID_REQUEST", "Month parameter is required");
        }

        YearMonth yearMonth = parseMonth(monthParam);
        String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());

        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid date format. Expected: yyyy-MM-dd");
        }
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month, MONTH_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid month format. Expected: yyyy-MM");
        }
    }
}
