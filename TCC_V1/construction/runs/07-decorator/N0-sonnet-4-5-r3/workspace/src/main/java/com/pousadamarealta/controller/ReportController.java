package com.pousadamarealta.controller;

import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null || dateParam.isBlank()) {
            throw new InvalidRequestException("Date parameter is required");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateParam, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format. Expected: yyyy-MM-dd");
        }

        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isBlank()) {
            throw new InvalidRequestException("Month parameter is required");
        }

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(monthParam, MONTH_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format. Expected: yyyy-MM");
        }

        String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }
}
