package com.pousadamarealta.controller;

import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.service.ReportService;
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
        if (dateParam == null || dateParam.isBlank()) {
            throw new InvalidRequestException("date parameter is required");
        }

        LocalDate date = parseDate(dateParam);
        String csv = reportService.generateOccupancyReport(date);

        ctx.contentType("text/csv; charset=utf-8")
           .result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isBlank()) {
            throw new InvalidRequestException("month parameter is required");
        }

        YearMonth month = parseMonth(monthParam);
        String csv = reportService.generateRevenueReport(month);

        ctx.contentType("text/csv; charset=utf-8")
           .result(csv);
    }

    public void getTourismTaxReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isBlank()) {
            throw new InvalidRequestException("month parameter is required");
        }

        YearMonth month = parseMonth(monthParam);
        String csv = reportService.generateTourismTaxReport(month);

        ctx.contentType("text/csv; charset=utf-8")
           .result(csv);
    }

    public void getRatePlansReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isBlank()) {
            throw new InvalidRequestException("month parameter is required");
        }

        YearMonth month = parseMonth(monthParam);
        String csv = reportService.generateRatePlansReport(month);

        ctx.contentType("text/csv; charset=utf-8")
           .result(csv);
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format: " + date);
        }
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month, MONTH_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format: " + month);
        }
    }
}
