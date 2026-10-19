package com.pousada.controller;

import com.pousada.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null || dateParam.isBlank()) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateParam, dateFormatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isBlank()) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        YearMonth month;
        try {
            month = YearMonth.parse(monthParam, monthFormatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        String csv = reportService.generateRevenueReport(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void getTourismTaxReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isBlank()) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        YearMonth month;
        try {
            month = YearMonth.parse(monthParam, monthFormatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        String csv = reportService.generateTourismTaxReport(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void getRatePlansReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isBlank()) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        YearMonth month;
        try {
            month = YearMonth.parse(monthParam, monthFormatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        String csv = reportService.generateRatePlansReport(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }
}
