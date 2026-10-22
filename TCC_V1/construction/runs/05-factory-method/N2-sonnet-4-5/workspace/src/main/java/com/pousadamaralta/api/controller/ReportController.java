package com.pousadamaralta.api.controller;

import com.pousadamaralta.api.dto.ErrorResponse;
import com.pousadamaralta.domain.service.ReportService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
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

        if (dateParam == null) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateParam);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
            return;
        }

        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void revenue(Context ctx) {
        String monthParam = ctx.queryParam("month");

        if (monthParam == null) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
            return;
        }

        YearMonth month;
        try {
            month = YearMonth.parse(monthParam);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
            return;
        }

        String csv = reportService.generateRevenueReport(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }
}
