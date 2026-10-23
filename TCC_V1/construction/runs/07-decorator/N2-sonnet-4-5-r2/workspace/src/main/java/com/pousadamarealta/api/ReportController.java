package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.ErrorResponse;
import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public final class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null || dateParam.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
                return;
            }

            LocalDate date = LocalDate.parse(dateParam);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthParam);
            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
